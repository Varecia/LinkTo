package com.tos.linkto.utils.yolo

import android.graphics.RectF

data class Detection(
    val classId: Int,
    val className: String,
    val confidence: Float,
    val boundingBox: RectF
)