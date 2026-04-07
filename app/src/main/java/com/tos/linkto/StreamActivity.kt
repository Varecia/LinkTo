package com.tos.linkto

import android.graphics.Bitmap
import android.os.Bundle
import android.view.SurfaceView
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tos.linkto.utils.video.VideoProcessingPipeline
import com.tos.linkto.utils.video.FrameRenderer
import kotlinx.coroutines.launch

class StreamActivity : AppCompatActivity() {
    private val videoUrl = "http://192.168.50.219:81/stream"
    private val imuUrl = "http://192.168.50.219:82/imu.json"
//    private val videoUrl = "http://10.0.2.2:8082/video"
//    private val videoUrl = "http://192.168.50.26:8082/video"
    private lateinit var surfaceView: SurfaceView
    private lateinit var pipeline: VideoProcessingPipeline
    private lateinit var renderer: FrameRenderer

    private var lastFrameTime = 0L
    private var fps = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        setContentView(R.layout.activity_stream)

        surfaceView = findViewById(R.id.surfaceView)

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startProcessing()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopProcessing()
            }
        })
    }

    private fun startProcessing() {
        renderer = FrameRenderer(surfaceView.holder)

        pipeline = VideoProcessingPipeline(this, videoUrl, useIMU = false)

        lifecycleScope.launch {
            pipeline.start { frame ->
                val now = System.currentTimeMillis()
                if (lastFrameTime > 0) {
                    val delta = now - lastFrameTime
                    fps = if (delta > 0) 1000f / delta else 0f
                }
                lastFrameTime = now

                renderer.renderFrame(
                    frame = frame,
                    fps = fps
                )
            }
        }

        Toast.makeText(this, "Processing started", Toast.LENGTH_SHORT).show()
    }

    private fun stopProcessing() {
        pipeline.stop()
        renderer.clear()
        Toast.makeText(this, "Processing stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProcessing()
    }
}