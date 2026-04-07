package com.tos.linkto.utils.slam

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class SlamManager(private val context: Context) {
    companion object {
        private const val TAG = "SlamManager"

        init {
            System.loadLibrary("crypto")
            System.loadLibrary("ssl")
            System.loadLibrary("slam_native")
        }
    }

    private var isInitialized = false
    private var useIMU = false

    private external fun nativeInit(vocabPath: String, configPath: String, useIMU: Boolean)
    private external fun nativeProcessFrame(data: ByteArray, width: Int, height: Int, timestamp: Long): FloatArray?
    private external fun nativeProcessIMU(
        accX: Float, accY: Float, accZ: Float,
        gyroX: Float, gyroY: Float, gyroZ: Float,
        timestamp: Long
    )

    private external fun nativeGetCameraPose(): FloatArray?
    private external fun nativeGetTrackingState(): Int
    private external fun nativeReset()
    private external fun nativeShutdown()

    fun initialize(useIMU: Boolean = false): Boolean {
        if (isInitialized) return true

        try {
            val vocabPath = copyAssetToFile("ORBvoc.txt")
            val configFile = if (useIMU) "config_mono_imu.yaml" else "config_mono.yaml"
            val configPath = copyAssetToFile(configFile)

            this.useIMU = useIMU
            nativeInit(vocabPath.absolutePath, configPath.absolutePath, useIMU)
            isInitialized = true
            Log.d(TAG, "SLAM initialized successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SLAM", e)
            return false
        }
    }

    fun processFrame(frameData: ByteArray, timestamp: Long): FloatArray? {
        if (!isInitialized) {
            Log.w(TAG, "SLAM not initialized")
            return null
        }
        if (frameData.isEmpty()) {
            Log.e(TAG, "Empty frame data")
            return null
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(frameData, 0, frameData.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            Log.e(TAG, "Invalid image dimensions: ${options.outWidth}x${options.outHeight}")
            return null
        }

        return nativeProcessFrame(frameData, options.outWidth, options.outHeight, timestamp)
    }

    fun processIMU(
        accX: Float, accY: Float, accZ: Float,
        gyroX: Float, gyroY: Float, gyroZ: Float,
        timestamp: Long
    ) {
        if (!isInitialized || !useIMU) return

        nativeProcessIMU(accX, accY, accZ, gyroX, gyroY, gyroZ, timestamp)
    }

    fun getCameraPose(): FloatArray? {
        if (!isInitialized) return null
        return nativeGetCameraPose()
    }

    fun getTrackingState(): Int {
        if (!isInitialized) return -1
        return nativeGetTrackingState()
    }

    fun reset() {
        if (isInitialized) {
            nativeReset()
        }
    }

    fun shutdown() {
        if (isInitialized) {
            nativeShutdown()
            isInitialized = false
        }
    }

    private fun copyAssetToFile(assetName: String): File {
        val destFile = File(context.filesDir, assetName)

        context.assets.open(assetName).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        Log.d(TAG, "Content of $assetName (first line):")
        destFile.bufferedReader().use { reader ->
            val firstLine = reader.readLine()
            Log.d(TAG, firstLine)
        }

        return destFile
    }
}