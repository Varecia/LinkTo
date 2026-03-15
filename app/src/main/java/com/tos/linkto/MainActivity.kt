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
import com.tos.linkto.ui.screen.HomeScreen
import com.tos.linkto.ui.screen.LinkScreen
import com.tos.linkto.ui.screen.UserScreen
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    companion object {
        lateinit var instance: MainActivity
            private set
    }

    lateinit var authRepo: AuthRepo
        private set

    // 引擎实例
    private var tts: TextToSpeech? = null
    private lateinit var speechRecognizer: SpeechRecognizer

    // 使用 Compose 状态来驱动 UI 更新
    private var recognizedText = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        instance = this
        authRepo = AuthRepo(this)

        // 初始化 TTS 和 语音识别
        tts = TextToSpeech(this, this)
        initSpeechRecognizer()

        setContent {
            // 1. 定义主题状态，默认跟随系统
            var isDarkTheme by remember { mutableStateOf(true) }
            // 2. 新增全局的无障碍模式状态（默认开启）
            var isAccessibilityMode by remember { mutableStateOf(true) }

            LinkToTheme(darkTheme = isDarkTheme) {
                LinkToApp(
                    tts = tts,
                    destinationText = recognizedText.value,
                    onStartSpeech = {checkPermissionAndStartSpeech()},
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = {isDarkTheme = !isDarkTheme},
                    isAccessibilityMode = isAccessibilityMode,
                    onAccessibilityToggle = { isAccessibilityMode = it}
                )
            }
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    recognizedText.value = text // 更新状态，UI 会自动重绘显示在搜索框
                    speak("已收到目的地：$text")
                }
            }
            override fun onReadyForSpeech(params: Bundle?) { Toast.makeText(this@MainActivity, "请说话...", Toast.LENGTH_SHORT).show() }
            override fun onError(error: Int) { speak("没听清，请重试") }
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
    }

    private fun checkPermissionAndStartSpeech() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            speak("请说出您的目的地")
            // 延迟启动，避免麦克风录入手机自己的说话声
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toString())
                }
                speechRecognizer.startListening(intent)
            }, 1500)
        }
    }

    fun speak(content: String) {
        tts?.speak(content, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.CHINESE
    }

    override fun onDestroy() {
        tts?.shutdown()
        speechRecognizer.destroy()
        super.onDestroy()
    }

    fun <T : Activity> raiseActivity(target: Class<T>) {
        val intent = Intent(this, target)
        startActivity(intent)
    }
}

@Composable
fun LinkToApp(
    tts: TextToSpeech?,
    destinationText: String,
    onStartSpeech: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    isAccessibilityMode: Boolean,
    onAccessibilityToggle: (Boolean) -> Unit
) {

    var lastClickTime by remember { mutableLongStateOf(0L) }

    var isLoggedIn by remember {
        mutableStateOf(MainActivity.instance.authRepo.isLoggedIn())
    }

//    if (!isLoggedIn) {
//        // 2. 如果未登录，直接显示登录注册页面
//        AuthScreen(
//            onAuthSuccess = {
//                // 当登录成功回调时，修改状态，Compose 会自动重绘并切换到主界面
//                isLoggedIn = true
//            }
//        )
//    } else {
        var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
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
                            if (isAccessibilityMode) {
                                val currentTime = System.currentTimeMillis()

                                // 判断逻辑：
                                if (currentTime - lastClickTime < 500) {
                                    // 【双击逻辑】：切换页面
                                    currentDestination = it
                                    android.util.Log.d("LinkToUI", "检测到双击：切换至 ${it.label}")
                                } else {
                                    // 【单击逻辑】：朗读名称
                                    // 直接调用 MainActivity 实例的 speak
                                    MainActivity.instance.speak(it.label)
                                    android.util.Log.d("LinkToUI", "检测到单击：朗读 ${it.label}")
                                }

                                lastClickTime = currentTime
                            }else{//普通模式
                                currentDestination = it
                            }
                        }
                    )
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(
                        tts = tts,
                        destinationText = destinationText,
                        onStartNav = { onStartSpeech() },
                        onStartAvoid = { MainActivity.instance.speak("正在开启避障") },
                        isAccessibilityMode = isAccessibilityMode,
                        onAccessibilityToggle = onAccessibilityToggle,
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
                            // 1. 清除本地登录状态
                            MainActivity.instance.authRepo.logout()
                            // 2. 改变 UI 状态，自动跳回登录页
                            isLoggedIn = false
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
