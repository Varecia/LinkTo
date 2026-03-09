package com.example.linkto

data class UserProfile(
    val name: String,
    val relativePhone: String
)

data class ProcessedFrame(
    val timestamp: Long = System.currentTimeMillis(),
    val yoloResult: String,
    val slamPose: List<Float>,
    val fusedLocation: String
)