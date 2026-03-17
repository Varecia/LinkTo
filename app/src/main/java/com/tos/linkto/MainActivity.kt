package com.tos.linkto

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tos.linkto.data.repo.AuthRepo
import com.tos.linkto.ui.screen.AuthScreen
import com.tos.linkto.ui.screen.HomeScreen
import com.tos.linkto.ui.screen.LinkScreen
import com.tos.linkto.ui.screen.UserScreen
import com.tos.linkto.ui.theme.LinkToTheme
import java.util.Locale
import androidx.compose.runtime.collectAsState
import com.tos.linkto.data.repo.SettingsRepo

class MainActivity : ComponentActivity(){
    companion object {
        lateinit var instance: MainActivity
            private set
    }

    lateinit var authRepo: AuthRepo
        private set
    lateinit var settingsRepo: SettingsRepo
        private set
    lateinit var voiceManager: VoiceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        instance = this

        authRepo = AuthRepo(this)
        settingsRepo = SettingsRepo(this)
        voiceManager = VoiceManager(this)

        setContent {
            val isDarkTheme by settingsRepo.isDarkTheme.collectAsState()
            val isAccessibilityMode by settingsRepo.isAccessibilityMode.collectAsState()
            val recognizedText by voiceManager.recognizedText

            LinkToTheme(darkTheme = isDarkTheme) {
                LinkToApp(
                    destinationText = recognizedText,
                    onStartSpeech = {checkPermissionAndStartSpeech()},
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { settingsRepo.toggleTheme() },
                    isAccessibilityMode = isAccessibilityMode,
                    onAccessibilityToggle = { settingsRepo.setAccessibilityMode(it) }
                )
            }
        }
    }

    private fun checkPermissionAndStartSpeech() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            voiceManager.startListening();
        }
    }

    // 快捷调用方法，方便全局使用 MainActivity.instance.speak()
    fun speak(content: String) = voiceManager.speak(content)

    override fun onDestroy() {
        voiceManager.destroy()
        super.onDestroy()
    }

    fun <T : Activity> raiseActivity(target: Class<T>) {
        val intent = Intent(this, target)
        startActivity(intent)
    }
}

@Composable
fun LinkToApp(
    destinationText: String,
    onStartSpeech: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    isAccessibilityMode: Boolean,
    onAccessibilityToggle: (Boolean) -> Unit
) {
    // 用 Map 为每个目的地存储最后点击时间，解决 remember 报错
    val navClickTimestamps = remember { mutableStateMapOf<AppDestinations, Long>() }

//    if (!MainActivity.instance.authRepo.isLoggedIn()) {
//        // 2. 如果未登录，直接显示登录注册页面
//        AuthScreen(
//            isAccessibilityMode = isAccessibilityMode,
//            onAuthSuccess = {}
//        )
//    } else {
        var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach { it ->
                    item(
                        icon = {
                            Icon(
                                painterResource(it.icon),
                                contentDescription = null
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = {
                            val currentTime = System.currentTimeMillis()
                            val lastTime = navClickTimestamps[it] ?: 0L
                            // 调用我们抽离出来的 Stupid 处理器
                            com.tos.linkto.ui.components.handleAccessibleClick(
                                isAccessibilityMode = isAccessibilityMode,
                                lastClickTime = lastTime,
                                label = it.label,
                                onAction = { currentDestination = it },
                                updateLastClickTime = { newTime -> navClickTimestamps[it] = newTime }
                            )
                        }
                    )
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(
                        destinationText = destinationText,
                        onStartNav = { onStartSpeech() },
                        onStartAvoid = { MainActivity.instance.speak("正在开启避障") },
                        isAccessibilityMode = isAccessibilityMode,
                    )
                    AppDestinations.LINK -> LinkScreen(
                        isAccessibilityMode = isAccessibilityMode,
                        onAccessibilityToggle = onAccessibilityToggle,
                    )
                    AppDestinations.USER -> UserScreen(
                        isDarkTheme = isDarkTheme, // 传入当前是否为深色模式的状态
                        onThemeToggle = onThemeToggle, // 传入切换主题的函数
                        isAccessibilityMode = isAccessibilityMode,
                        onAccessibilityToggle = onAccessibilityToggle,
                        onLogout = {
                            // 注意：这里的登出逻辑改由 UserScreen 内部处理确认弹窗
                        }
                    )
                }
            }
        }
    }

//}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("首页", R.drawable.ic_home),
    LINK("关联", R.drawable.ic_favorite),
    USER("我的", R.drawable.ic_account_box),
}
