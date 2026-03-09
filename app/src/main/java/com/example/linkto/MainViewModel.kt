package com.example.linkto

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    // 注入 viewModelScope，确保引擎后台上传协程能随 ViewModel 销毁而停止
    private val perceptionEngine = PerceptionEngine(viewModelScope)

    var isRunning by mutableStateOf(false)
    var latestData by mutableStateOf<ProcessedFrame?>(null)
    var currentUser by mutableStateOf<UserProfile?>(null)

    fun toggleStreaming() {
        isRunning = !isRunning
        if (isRunning) {
            viewModelScope.launch {
                while (isRunning) {
                    // 获取计算结果，内部已自动处理 uploadChannel.trySend
                    latestData = perceptionEngine.runFastInference()
                    delay(100)
                }
            }
        }
    }

    // 可选：手动同步用户信息到云端
    fun syncProfile() {
        val user = currentUser ?: return
        viewModelScope.launch {
            try {
                NetworkClient.apiService.uploadUserProfile(user)
            } catch (e: Exception) {
                println("用户配置上传失败: ${e.message}")
            }
        }
    }
}