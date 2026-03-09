package com.example.linkto

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import retrofit2.Response

class PerceptionEngine(scope: CoroutineScope) {
    // 1. 调用合并后的网络模块
    private val api = NetworkClient.apiService

    // 2. 异步上传管道
    private val uploadChannel = Channel<ProcessedFrame>(Channel.BUFFERED)

    init {
        // 启动后台上传协程，绑定到外部传入的 scope (通常是 ViewModelScope)
        scope.launch(Dispatchers.IO) {
            for (frame in uploadChannel) {
                try {
                    // 执行上传请求
                    val response: Response<Unit> = api.uploadSensorData(frame)

                    if (response.isSuccessful) {
                        // 【功能增强】解析服务器返回的控制信号
                        // 假设服务器返回了一些控制指令
                        val signal = response.body()
                        handleCloudSignal(signal)
                        println("云端同步成功，时间戳: ${frame.timestamp}")
                    } else {
                        println("服务器拒绝请求: ${response.code()}")
                    }
                } catch (e: Exception) {
                    // 网络断开、域名解析失败等
                    println("网络链路异常: ${e.message}")
                }
            }
        }
    }

    /**
     * 处理来自云端的指令（信号转换逻辑）
     */
    private fun handleCloudSignal(signal: Any?) {
        // TODO: 根据云端下发的指令，控制本地硬件或更新 UI 状态
        if (signal != null) {
            println("收到云端信号: $signal")
        }
    }

    /**
     * 并行计算 YOLO 和 SLAM
     */
    suspend fun runFastInference(): ProcessedFrame = withContext(Dispatchers.Default) {
        // 并行执行算法，总耗时由最慢的那个决定
        val yoloJob = async { simulateYOLO() }
        val slamJob = async { simulateSLAM() }

        val frame = ProcessedFrame(
            yoloResult = yoloJob.await(),
            slamPose = slamJob.await(),
            fusedLocation = "经纬度:116.39, 39.91",
            timestamp = System.currentTimeMillis()
        )

        // 【关键】将数据推入管道。trySend 不会挂起，保证了 10Hz 的计算循环不被网络波动干扰
        val result = uploadChannel.trySend(frame)
        if (result.isFailure) {
            println("上传队列已满，丢弃当前帧")
        }

        frame
    }

    private suspend fun simulateYOLO(): String { delay(50); return "Detected_Obstacle" }
    private suspend fun simulateSLAM(): List<Float> { delay(30); return listOf(1f, 2f, 3f) }
}
