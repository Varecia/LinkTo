package com.example.linkto

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tos.linkto.R
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false

    // UI 容器
    private lateinit var containerNav: View
    private lateinit var containerFamily: View
    private lateinit var containerSettings: View
    private lateinit var bottomNav: BottomNavigationView

    // 语音识别相关
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent

    //搜索框
    private lateinit var etSearchDestination: android.widget.EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 初始化组件
        initViews()
        initTTS()
        initSpeechRecognizer()

        // 2. 绑定交互逻辑
        setupInteractions()
    }

    private fun initViews() {
        containerNav = findViewById(R.id.container_navigation)
        containerFamily = findViewById(R.id.container_family)
        containerSettings = findViewById(R.id.container_settings)
        bottomNav = findViewById(R.id.bottom_navigation)
        // 初始化搜索框
        etSearchDestination = findViewById(R.id.et_search_destination)

        // 同样为搜索框添加“单击朗读”逻辑，让用户知道当前输入了什么
        setCustomAccessibleListener(
            etSearchDestination,
            onSingleTap = {
                // 【单击逻辑】：动态获取搜索框内容并朗读
                val currentText = etSearchDestination.text.toString()
                if (currentText.isNotEmpty()) {
                    speak("当前目的地是：$currentText，重新输入请双击重试")
                } else {
                    speak("搜索框为空，请双击搜索框或开始导航按钮来输入目的地")
                }
            },
            onDoubleTap = {
                // 【双击逻辑】：你可以定义双击搜索框做什么，比如清除内容，或者再次启动语音识别
                speak("请说出您的目的地")
                checkPermissionAndStartSpeech() // 调用你之前写的启动语音识别的方法
            }
        )
    }

    private fun initTTS() {
        tts = TextToSpeech(this, this)
    }

    private fun setupInteractions() {
        val btnStartNav = findViewById<Button>(R.id.btn_start_navigation)
        val btnStartAvoid = findViewById<Button>(R.id.btn_start_obstacle_avoidance)

        // 设置“开始导航”按钮：单击读，双击听目的地
        setCustomAccessibleListener(
            btnStartNav,
            onSingleTap = { speak("开始导航") },
            onDoubleTap = { checkPermissionAndStartSpeech() }
        )

        // 设置“开始避障”按钮：单击读，双击开启
        setCustomAccessibleListener(
            btnStartAvoid,
            onSingleTap = { speak("开始避障") },
            onDoubleTap = {
                speak("避障功能正在启动")
                // TODO: 这里放置启动摄像头避障的 Activity 或逻辑
            }
        )

        // 底部导航栏逻辑
        bottomNav.post {
            for (i in 0 until bottomNav.menu.size()) {
                val menuItem = bottomNav.menu.getItem(i)
                val id = menuItem.itemId
                val view = findViewById<View>(id)
                val label = menuItem.title.toString()

                setCustomAccessibleListener(
                    view,
                    onSingleTap = {
                        // 单击时朗读：例如“导航页面”
                        speak("${label}页面")
                    },
                    onDoubleTap = {
                        // 双击时切换页面
                        switchPage(id)
                    }
                )
            }
        }
        // 屏蔽原生点击逻辑，避免干扰我们的双击
        bottomNav.setOnItemSelectedListener { false }
    }

    private fun switchPage(itemId: Int) {
        containerNav.visibility = View.GONE
        containerFamily.visibility = View.GONE
        containerSettings.visibility = View.GONE

        when (itemId) {
            R.id.nav_navigation -> {
                containerNav.visibility = View.VISIBLE
                bottomNav.selectedItemId = R.id.nav_navigation
                speak("已进入导航主页")
            }
            R.id.nav_family -> {
                containerFamily.visibility = View.VISIBLE
                bottomNav.selectedItemId = R.id.nav_family
                speak("已进入家人页面")
            }
            R.id.nav_settings -> {
                containerSettings.visibility = View.VISIBLE
                bottomNav.selectedItemId = R.id.nav_settings
                speak("已进入设置页面")
            }
        }
    }

    /**
     * 自定义手势监听：实现单击朗读，双击确认
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCustomAccessibleListener(view: View, onSingleTap: () -> Unit, onDoubleTap: () -> Unit) {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onSingleTap()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap()
                return true
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })

        view.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
        }
    }

    // --- 语音识别部分 ---

    private fun initSpeechRecognizer() {
        // 创建识别器实例
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // 配置识别参数
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toString())
            // 设置识别超时时间或是否允许部分结果
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        // 设置识别回调
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // 当识别器准备好听用户说话时（TTS 播报完毕后触发）
                Toast.makeText(this@MainActivity, "请开始说话...", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {
                // 用户开始说话
            }

            override fun onResults(results: Bundle?) {
                // 识别成功，获取最终结果
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val destination = matches[0] // 获取最匹配的一个结果
                    handleDestinationFound(destination)
                }
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "没听清，请重新双击重试"
                    SpeechRecognizer.ERROR_NETWORK -> "网络连接失败"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器正忙"
                    else -> "语音识别出错，请重试"
                }
                speak(message)
            }

            // 以下是必须实现但此处暂不需要处理的回调
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun checkPermissionAndStartSpeech() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            startDestinationFlow()
        }
    }

    private fun startDestinationFlow() {
        // 先用语音问用户
        speak("请说出您的目的地")

        // 注意：我们希望 TTS 讲完之后才开始录音。
        // 在实际开发中，可以使用 TTS 的 OnUtteranceProgressListener 监听播报结束。
        // 这里简单处理：通过延迟或者直接在 speak 之后开启（部分手机会自动处理回声消除）

        // 延迟 1.5 秒开启录音，确保 TTS 已经说完了“请说出您的目的地”
        window.decorView.postDelayed({
            speechRecognizer.startListening(recognizerIntent)
        }, 1500)
    }

    private fun handleDestinationFound(destination: String) {
        // 1. 将目的地文本显示到屏幕顶部的搜索框中
        etSearchDestination.setText(destination)

        // 反馈给用户
        speak("已收到，目的地是：$destination。正在为您规划路线。")

        // TODO: 这里调用地图 SDK 的 API 进行路径搜索
        // searchRoute(destination)
    }

    // --- TTS 回调 ---

    private fun speak(content: String) {
        if (isTtsInitialized) {
            tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, "ID")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.CHINESE
            isTtsInitialized = true
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
        super.onDestroy()
    }
}