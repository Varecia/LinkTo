package com.tos.linkto.data.repo

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepo(context: Context) {
    // 使用 SharedPreferences 进行本地持久化存储
    private val prefs: SharedPreferences = context.getSharedPreferences("linkto_settings", Context.MODE_PRIVATE)

    // ================= 主题模式 =================
    // 默认开启深色模式 (true)
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        prefs.edit().putBoolean("is_dark_theme", newValue).apply() // 存入本地
        _isDarkTheme.value = newValue // 触发 UI 刷新
    }

    // ================= 无障碍模式 =================
    // 默认开启无障碍模式 (true)
    private val _isAccessibilityMode = MutableStateFlow(prefs.getBoolean("is_accessibility_mode", true))
    val isAccessibilityMode: StateFlow<Boolean> = _isAccessibilityMode.asStateFlow()

    fun setAccessibilityMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_accessibility_mode", enabled).apply() // 存入本地
        _isAccessibilityMode.value = enabled // 触发 UI 刷新
    }
}