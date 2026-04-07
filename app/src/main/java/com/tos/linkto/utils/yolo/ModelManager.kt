package com.tos.linkto.utils.yolo

import android.content.Context

class ModelManager(private val context: Context) {

    private var currentConfig: ModelConfig = CocoModelConfig()
    private var classNames: List<String> = emptyList()

    fun setModelConfig(config: ModelConfig) {
        currentConfig = config
        classNames = config.loadClassNames(context)
    }

    fun getCurrentConfig(): ModelConfig = currentConfig

    fun getClassNames(): List<String> = classNames

    fun getNumClasses(): Int = classNames.size

    fun isModelReady(): Boolean {
        return try {
            context.assets.open(currentConfig.modelFileName).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getModelInfo(): String = """
        Model: ${currentConfig.modelFileName}
        Input Size: ${currentConfig.inputSize}x${currentConfig.inputSize}
        Classes: ${classNames.size}
        Confidence Threshold: ${currentConfig.confidenceThreshold}
        NMS Threshold: ${currentConfig.nmsThreshold}
    """.trimIndent()
}