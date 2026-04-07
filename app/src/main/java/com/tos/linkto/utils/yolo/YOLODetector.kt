package com.tos.linkto.utils.yolo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import androidx.core.graphics.scale
import org.tensorflow.lite.gpu.CompatibilityList

class YOLODetector(
    private val context: Context,
    private val modelManager: ModelManager
) {
    companion object {
        private const val TAG = "YOLODetector"
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var currentModelConfig: ModelConfig? = null

    fun loadModel(): Boolean {
        if (!modelManager.isModelReady()) {
            return false
        }

        val config = modelManager.getCurrentConfig()
        currentModelConfig = config

        return try {
            val modelBuffer = loadModelFile(config.modelFileName)

            val compatList = CompatibilityList()
            val delegate: GpuDelegate? = if (compatList.isDelegateSupportedOnThisDevice) {
                GpuDelegate().also {
                    gpuDelegate = it
                }
            } else {
                Log.w(TAG, "GPU not available")
                null
            }
            val options = Interpreter.Options().apply {
                setUseXNNPACK(true)
                setNumThreads(4)
                delegate?.let { addDelegate(gpuDelegate) }
            }

            interpreter?.close()
            interpreter = Interpreter(modelBuffer, options)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            false
        }
    }

    fun switchModel(newConfig: ModelConfig): Boolean {
        modelManager.setModelConfig(newConfig)
        return loadModel()
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val interpreter = interpreter ?: return emptyList()
        val config = currentModelConfig ?: return emptyList()

        val resizedBitmap = bitmap.scale(config.inputSize, config.inputSize)
        val inputBuffer = bitmapToByteBuffer(resizedBitmap, config.inputSize)

        val outputShape = intArrayOf(1, 84, 8400)
        val outputBuffer = Array(1) { Array(84) { FloatArray(8400) } }

        interpreter.run(inputBuffer, outputBuffer)

        val detections = postProcess(
            outputBuffer,
            bitmap.width,
            bitmap.height,
            config
        )

        resizedBitmap.recycle()

        return detections
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap, inputSize: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }

        return buffer
    }

    private fun postProcess(
        output: Array<Array<FloatArray>>,
        originalWidth: Int,
        originalHeight: Int,
        config: ModelConfig
    ): List<Detection> {
        val classNames = modelManager.getClassNames()
        val detections = mutableListOf<Detection>()

        for (i in 0 until 8400) {
            val confidence = output[0][4][i]
            if (confidence > config.confidenceThreshold) {
                var maxClassScore = 0f
                var classId = 0
                for (c in 5 until 84) {
                    val score = output[0][c][i]
                    if (score > maxClassScore) {
                        maxClassScore = score
                        classId = c - 5
                    }
                }

                val finalConfidence = confidence * maxClassScore
                if (finalConfidence > config.confidenceThreshold && classId < classNames.size) {
                    val cx = output[0][0][i]
                    val cy = output[0][1][i]
                    val w = output[0][2][i]
                    val h = output[0][3][i]

                    val scaleX = originalWidth.toFloat() / config.inputSize
                    val scaleY = originalHeight.toFloat() / config.inputSize

                    val x = (cx - w / 2) * scaleX
                    val y = (cy - h / 2) * scaleY
                    val width = w * scaleX
                    val height = h * scaleY

                    detections.add(
                        Detection(
                            classId = classId,
                            className = classNames[classId],
                            confidence = finalConfidence,
                            boundingBox = RectF(x, y, x + width, y + height)
                        )
                    )
                }
            }
        }

        return nonMaxSuppression(detections, config.nmsThreshold)
    }

    private fun nonMaxSuppression(
        detections: List<Detection>,
        nmsThreshold: Float
    ): List<Detection> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.confidence }
        val result = mutableListOf<Detection>()
        val suppressed = BooleanArray(detections.size)

        for (i in sorted.indices) {
            if (suppressed[i]) continue
            result.add(sorted[i])

            for (j in i + 1 until sorted.size) {
                if (suppressed[j]) continue
                val iou = calculateIOU(sorted[i].boundingBox, sorted[j].boundingBox)
                if (iou > nmsThreshold) {
                    suppressed[j] = true
                }
            }
        }

        return result
    }

    private fun calculateIOU(rect1: RectF, rect2: RectF): Float {
        val overlapLeft = maxOf(rect1.left, rect2.left)
        val overlapTop = maxOf(rect1.top, rect2.top)
        val overlapRight = minOf(rect1.right, rect2.right)
        val overlapBottom = minOf(rect1.bottom, rect2.bottom)

        if (overlapRight <= overlapLeft || overlapBottom <= overlapTop) {
            return 0f
        }

        val overlapArea = (overlapRight - overlapLeft) * (overlapBottom - overlapTop)
        val area1 = (rect1.right - rect1.left) * (rect1.bottom - rect1.top)
        val area2 = (rect2.right - rect2.left) * (rect2.bottom - rect2.top)

        return overlapArea / (area1 + area2 - overlapArea)
    }

    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun release() {
        interpreter?.close()
        gpuDelegate?.close()
    }
}