package com.tos.linkto.utils.video

import android.graphics.*
import android.view.SurfaceHolder

class FrameRenderer(private val surfaceHolder: SurfaceHolder) {

    private val boundingBoxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val labelBgPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val trajectoryPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val infoPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 28f
        isAntiAlias = true
        typeface = Typeface.MONOSPACE
    }

    private val trajectoryPoints = mutableListOf<Pair<Float, Float>>()

    fun renderFrame(
        frame : RenderFrame,
        fps: Float = 0f
    ) {
        val canvas = surfaceHolder.lockCanvas() ?: return

        try {
            val dstRect = Rect(0, 0, canvas.width, canvas.height)
            canvas.drawBitmap(frame.bitmap, null, dstRect, null)

            if (frame.yoloResult != null) {
                for (detection in frame.yoloResult.detections) {
                    val scaledRect = scaleRectToSurface(detection.boundingBox, frame.bitmap, canvas)
                    canvas.drawRect(scaledRect, boundingBoxPaint)

                    val label = "${detection.className} ${(detection.confidence * 100).toInt()}%"
                    drawLabel(canvas, label, scaledRect.left, scaledRect.top - 10, labelPaint)
                }
            }

            if (frame.cameraPose != null && frame.cameraPose.size >= 16) {
                updateTrajectory(frame.cameraPose, canvas)
                drawTrajectory(canvas)
            }

            drawPerformanceInfo(canvas, fps, frame.slamTime, frame.yoloResult)

            drawTrackingInfo(canvas, frame.cameraPose, frame.trackingState)

        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun scaleRectToSurface(rect: RectF, sourceBitmap: Bitmap, canvas: Canvas): RectF {
        val scaleX = canvas.width.toFloat() / sourceBitmap.width
        val scaleY = canvas.height.toFloat() / sourceBitmap.height

        return RectF(
            rect.left * scaleX,
            rect.top * scaleY,
            rect.right * scaleX,
            rect.bottom * scaleY
        )
    }

    private fun drawLabel(canvas: Canvas, text: String, x: Float, y: Float, textPaint: Paint) {
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)

        canvas.drawRect(
            x - 5,
            y - textBounds.height() - 5,
            x + textBounds.width() + 10,
            y + 5,
            labelBgPaint
        )
        canvas.drawText(text, x, y, textPaint)
    }

    private fun updateTrajectory(poseMatrix: FloatArray, canvas: Canvas) {
        // 提取平移向量
        val x = poseMatrix[12]
        val z = poseMatrix[14]

        // 转换到屏幕坐标
        val screenX = (x * 15 + canvas.width / 2)
        val screenY = (z * 15 + canvas.height - 150)

        trajectoryPoints.add(Pair(screenX, screenY))

        // 限制轨迹长度
        while (trajectoryPoints.size > 300) {
            trajectoryPoints.removeAt(0)
        }
    }

    private fun drawTrajectory(canvas: Canvas) {
        if (trajectoryPoints.size < 2) return

        val path = Path()
        path.moveTo(trajectoryPoints[0].first, trajectoryPoints[0].second)

        for (i in 1 until trajectoryPoints.size) {
            path.lineTo(trajectoryPoints[i].first, trajectoryPoints[i].second)
        }

        canvas.drawPath(path, trajectoryPaint)

        // 绘制当前点
        val lastPoint = trajectoryPoints.lastOrNull()
        if (lastPoint != null) {
            val pointPaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.FILL
            }
            canvas.drawCircle(lastPoint.first, lastPoint.second, 12f, pointPaint)
        }
    }

    private fun drawPerformanceInfo(canvas: Canvas, fps: Float, slamTime: Long, yoloResult: YOLOResult?) {
        var y = 50f

        val info = mutableListOf(
            "FPS: ${String.format("%.1f", fps)}",
            "SLAM: ${slamTime}ms"
        )

        if (yoloResult != null) {
            info.add("YOLO: ${yoloResult.inferenceTime}ms (frame ${yoloResult.frameIndex})")
            info.add("Objects: ${yoloResult.detections.size}")
        } else {
            info.add("YOLO: waiting...")
        }

        for (line in info) {
            canvas.drawText(line, 20f, y, infoPaint)
            y += 35f
        }
    }

    private fun drawTrackingInfo(canvas: Canvas, cameraPose: FloatArray?, trackingState: Int) {
        val stateText = when (trackingState) {
            1 -> "TRACKING GOOD"
            0 -> "TRACKING LOST"
            else -> "INITIALIZING"
        }

        val stateColor = when (trackingState) {
            1 -> Color.GREEN
            0 -> Color.RED
            else -> Color.YELLOW
        }

        val statePaint = Paint().apply {
            color = stateColor
            textSize = 30f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        canvas.drawText(stateText, canvas.width - 250f, 80f, statePaint)

        if (cameraPose != null && cameraPose.size >= 16) {
            val x = cameraPose[12]
            val y = cameraPose[13]
            val z = cameraPose[14]
            val posText = String.format("Pos: (%.2f, %.2f, %.2f)", x, y, z)
            canvas.drawText(posText, canvas.width - 250f, 120f, infoPaint)
        }
    }

    fun clear() {
        trajectoryPoints.clear()
    }
}