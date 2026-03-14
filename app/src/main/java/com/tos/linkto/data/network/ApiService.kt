package com.tos.linkto.data.network

import com.tos.linkto.data.model.*
import retrofit2.http.*

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("api/user/profile")
    suspend fun getProfile(@Header("Authorization") token: String): User
}