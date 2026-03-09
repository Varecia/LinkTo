package com.example.linkto

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

// --- 1. 配置中心 ---
object NetworkConfig {
    const val BASE_URL = "http://192.168.1.100:8080/" // 记得填入实际IP
}

// --- 2. 接口协议 ---
interface ApiService {
    @POST("sensor/upload") // 建议填入具体路径
    suspend fun uploadSensorData(@Body data: ProcessedFrame): Response<Unit>

    @POST("user/sync")
    suspend fun uploadUserProfile(@Body user: UserProfile): Response<Unit>
}

// --- 3. 网络客户端---
object NetworkClient {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}