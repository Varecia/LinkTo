package com.tos.linkto.data.repo

import android.content.Context
import android.content.SharedPreferences
import com.tos.linkto.data.model.User
import com.tos.linkto.data.model.LoginRequest
import com.tos.linkto.data.model.RegisterRequest
import com.tos.linkto.data.network.RetrofitClient
import androidx.core.content.edit

class AuthRepo(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val apiService = RetrofitClient.apiService

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            saveAuthData(response.user, response.token)
            Result.success(response.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<User> {
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            saveAuthData(response.user, response.token)
            Result.success(response.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveAuthData(user: User, token: String) {
        sharedPref.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_NAME, user.username)
            apply()
        }
    }

    fun getToken(): String? = sharedPref.getString(KEY_TOKEN, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun logout() {
        sharedPref.edit { clear() }
    }

    fun getCurrentUser(): User? {
        val token = getToken() ?: return null
        return User(
            id = sharedPref.getString(KEY_USER_ID, "") ?: "",
            username = sharedPref.getString(KEY_USER_NAME, "") ?: "",
            email = sharedPref.getString(KEY_USER_EMAIL, "") ?: "",
            token = token
        )
    }
}