package com.tos.linkto.utils.yolo

import android.content.Context

interface ModelConfig {
    val modelFileName: String
    val inputSize: Int
    val confidenceThreshold: Float
    val nmsThreshold: Float

    fun loadClassNames(context: Context): List<String>
}

class CocoModelConfig : ModelConfig {
    override val modelFileName = "yolov8n_float32.tflite"
    override val inputSize = 640
    override val confidenceThreshold = 0.5f
    override val nmsThreshold = 0.45f

    override fun loadClassNames(context: Context): List<String> {
        return try {
            context.assets.open("coco_labels.txt")
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            defaultCocoClasses()
        }
    }

    private fun defaultCocoClasses(): List<String> = listOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck",
        "boat", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench",
        "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra",
        "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove",
        "skateboard", "surfboard", "tennis racket", "bottle", "wine glass", "cup",
        "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
        "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse",
        "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
        "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
        "toothbrush"
    )
}

/**
 * 如果有下一阶段的话，收集专用数据训练
 */
class CustomModelConfig(
    override val modelFileName: String = "custom_model.tflite",
    override val inputSize: Int = 416,
    override val confidenceThreshold: Float = 0.5f,
    override val nmsThreshold: Float = 0.45f,
    private val labelsFile: String = "custom_labels.txt"
) : ModelConfig {

    override fun loadClassNames(context: Context): List<String> {
        return try {
            context.assets.open(labelsFile)
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            throw IllegalArgumentException("Custom labels file '$labelsFile' not found in assets")
        }
    }

    data class TrainingConfig(
        val datasetPath: String,
        val epochs: Int = 100,
        val batchSize: Int = 16,
        val imgSize: Int = 640,
        val numClasses: Int,
        val classNames: List<String>
    )
}