package com.tos.linkto.utils.video

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.tos.linkto.utils.slam.SlamManager
import com.tos.linkto.utils.yolo.CocoModelConfig
import com.tos.linkto.utils.yolo.Detection
import com.tos.linkto.utils.yolo.ModelConfig
import com.tos.linkto.utils.yolo.ModelManager
import com.tos.linkto.utils.yolo.YOLODetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

data class FrameData(
    val jpegData: ByteArray,
    val bitmap: Bitmap,
    val timestamp: Long,
    val frameIndex: Int
)

data class YOLOResult(
    val frameIndex: Int,
    val timestamp: Long,
    val detections: List<Detection>,
    val inferenceTime: Long
)

data class SLAMResult(
    val frameIndex: Int,
    val timestamp: Long,
    val cameraPose: FloatArray?,
    val trackingState: Int,
    val processingTime: Long
)

data class RenderFrame(
    val frameIndex: Int,
    val timestamp: Long,
    val bitmap: Bitmap,
    val cameraPose: FloatArray?,
    val trackingState: Int,
    val slamTime: Long,
    val yoloResult: YOLOResult?
)

class VideoProcessingPipeline(
    private val context: Context,
    private val videoUrl: String,
    private val useIMU: Boolean = false,
    private val modelConfig: ModelConfig = CocoModelConfig()
) {
    companion object {
        private const val TAG = "ProcessingPipeline"
        private const val YOLO_FRAME_SKIP_INTERVAL = 2
    }

    private val _frame = MutableStateFlow<RenderFrame?>(null)

    private val frameChannel = Channel<FrameData>(capacity = 10)
    private var currentYoloResult: YOLOResult? = null
    private var lastYoloFrameIndex = -1

    private var slamManager: SlamManager? = null
    private var yoloDetector: YOLODetector? = null
    private var frameExtractor: FrameExtractor? = null

    private val isRunning = AtomicBoolean(false)
    private val isShuttingDown = AtomicBoolean(false)
    private var lastFrameIndex = -1

    private val averageSlamTime = AtomicLong(0)
    private val averageYoloTime = AtomicLong(0)
    private var slamSampleCount = 0
    private var yoloSampleCount = 0

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    suspend fun start(onFrame: (RenderFrame) -> Unit) {
        if (isRunning.getAndSet(true)) return

        slamManager = SlamManager(context).apply {
            if (initialize(useIMU).not()) {
                Log.e(TAG, "Failed to initialize SLAM")
                return
            }
        }

        scope.launch {
            val modelManager = ModelManager(context).apply {
                setModelConfig(modelConfig)
            }

            yoloDetector = YOLODetector(context, modelManager).apply {
                if (!loadModel()) {
                    Log.e(TAG, "Failed to load model: ${modelManager.getCurrentConfig().modelFileName}")
                } else {
                    Log.d(TAG, "Model loaded: ${modelManager.getModelInfo()}")
                }
            }
        }

        scope.launch { processSLAM() }
        scope.launch { processYOLO() }
        scope.launch {
            _frame.collect { frame ->
                if (frame != null && isShuttingDown.get().not()) {
                    onFrame(frame)
                }
            }
        }

        frameExtractor = FrameExtractor(videoUrl) { jpegData, bitmap, timestamp, frameIndex ->
            if (isRunning.get() && isShuttingDown.get().not()) {
                runBlocking {
                    frameChannel.send(FrameData(jpegData, bitmap, timestamp, frameIndex))
                }
            }
        }

        frameExtractor?.start()

        Log.d(TAG, "Pipeline started")
    }

    private suspend fun processSLAM() {
        for (frame in frameChannel) {
            if (isRunning.get().not() || isShuttingDown.get()) break

            try {
                val startTime = System.nanoTime()

                val pose = slamManager?.processFrame(frame.jpegData, frame.timestamp)
                val trackingState = slamManager?.getTrackingState() ?: -1
                val processingTime = (System.nanoTime() - startTime) / 1_000_000

                slamSampleCount++
                averageSlamTime.set(
                    (averageSlamTime.get() * (slamSampleCount - 1) + processingTime) / slamSampleCount
                )

                val yoloResultToUse = currentYoloResult

                val renderFrameData = RenderFrame(
                    frameIndex = frame.frameIndex,
                    timestamp = frame.timestamp,
                    bitmap = frame.bitmap,
                    cameraPose = pose,
                    trackingState = trackingState,
                    slamTime = processingTime,
                    yoloResult = yoloResultToUse,
                )

                _frame.value = renderFrameData

                val yoloInfo = if (yoloResultToUse != null) {
                    "YOLO from frame ${yoloResultToUse.frameIndex}, ${yoloResultToUse.detections.size} objects"
                } else {
                    "No YOLO result yet"
                }
                Log.d(TAG, "SLAM Frame ${frame.frameIndex}: ${processingTime}ms, State: $trackingState, $yoloInfo")

                lastFrameIndex = frame.frameIndex
            } catch (e: Exception) {
                Log.e(TAG, "Error in processSLAM", e)
                if (isShuttingDown.get()) break
            }
        }
    }

    private suspend fun processYOLO() {
        var frameCounter = 0

        for (frame in frameChannel) {
            if (isRunning.get().not() || isShuttingDown.get()) break

            frameCounter++

            if (frameCounter % YOLO_FRAME_SKIP_INTERVAL != 0) {
                continue
            }

            var retryCount = 0
            while (yoloDetector == null && isRunning.get() && !isShuttingDown.get() && retryCount < 50) {
                delay(20)
                retryCount++
            }

            val detector = yoloDetector
            if (detector == null || isShuttingDown.get()) continue

            try {
                val startTime = System.nanoTime()

                val detections = withContext(Dispatchers.Default) {
                    detector.detect(frame.bitmap)
                }

                val inferenceTime = (System.nanoTime() - startTime) / 1_000_000

                yoloSampleCount++
                averageYoloTime.set(
                    (averageYoloTime.get() * (yoloSampleCount - 1) + inferenceTime) / yoloSampleCount
                )

                currentYoloResult = YOLOResult(
                    frameIndex = frame.frameIndex,
                    timestamp = frame.timestamp,
                    detections = detections,
                    inferenceTime = inferenceTime
                )
                lastYoloFrameIndex = frame.frameIndex

                Log.d(
                    TAG, "YOLO Updated - Frame ${frame.frameIndex}: ${detections.size} objects, " +
                            "${inferenceTime}ms (avg: ${averageYoloTime.get()}ms)"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in processYOLO", e)
            }

        }
    }

    fun getPerformanceStats(): Pair<Long, Long> {
        return Pair(averageSlamTime.get(), averageYoloTime.get())
    }

    fun stop() {
        if(isRunning.getAndSet(false).not()) return
        if (isShuttingDown.getAndSet(true)) return

        frameExtractor?.stop()
        frameExtractor = null

        frameChannel.close()

        runBlocking { delay(200) }

        job.cancel()

        runBlocking(Dispatchers.IO) {
            try {
                slamManager?.shutdown()
            } catch (e: Exception) {
                Log.e(TAG, "Error shutting down SLAM", e)
            }
            slamManager = null
        }

        runBlocking(Dispatchers.IO) {
            try {
                yoloDetector?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing YOLO", e)
            }
            yoloDetector = null
        }

        currentYoloResult = null

        Log.d(TAG, "Pipeline stopped")
    }
}