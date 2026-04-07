package com.tos.linkto.utils.video

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class FrameExtractor(
    private val videoUrl: String,
    private val onFrame: (ByteArray, Bitmap, Long, Int) -> Unit
) {
    companion object {
        private const val TAG = "FrameExtractor"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val isRunning = AtomicBoolean(false)
    private var frameIndex = 0

    suspend fun start() {
        isRunning.set(true)

        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(videoUrl)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Failed to connect: ${response.code}")
                }

                val body = response.body ?: return@withContext
                val inputStream = body.byteStream()

                parseStream(inputStream)
            }
        }
    }

    private suspend fun parseStream(inputStream: InputStream) {
        val buffer = ByteArray(4096)
        var state = 0
        val frameData = ByteArrayOutputStream()

        withContext(Dispatchers.IO){
            while (isRunning.get()) {
                val len = inputStream.read(buffer)
                if (len == -1) break
                for (i in 0 until len) {
                    val b = buffer[i].toInt() and 0xFF
                    when (state) {
                        0 -> if (b == 0xFF) state = 1
                        1 -> {
                            if (b == 0xD8) {
                                frameData.reset()
                                frameData.write(0xFF)
                                frameData.write(0xD8)
                                state = 2
                            } else {
                                state = 0
                            }
                        }

                        2 -> {
                            frameData.write(b)
                            if (b == 0xD9 && frameData.size() >= 2 &&
                                (frameData.toByteArray()[frameData.size() - 2].toInt() and 0xFF) == 0xFF
                            ) {
                                val jpeg = frameData.toByteArray()
                                val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                                if (bitmap != null) {
                                    val timestamp = System.nanoTime()
                                    val currentFrame = frameIndex++
                                    Log.d(TAG,"Sending frame ${currentFrame} at ts=${timestamp}")
                                    onFrame(jpeg, bitmap, timestamp, currentFrame)
                                }
                                state = 0
                            }
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        isRunning.set(false)
    }
}