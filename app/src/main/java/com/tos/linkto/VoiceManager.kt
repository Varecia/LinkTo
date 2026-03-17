package com.tos.linkto

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import java.util.Locale

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    // 暴露出识别到的文本，UI 可以直接观察它
    var recognizedText = mutableStateOf("")
        private set

    init {
        tts = TextToSpeech(context, this)
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    recognizedText.value = text
                    speak("已收到目的地：$text")
                }
            }
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(context, "请说话...", Toast.LENGTH_SHORT).show()
            }
            override fun onError(error: Int) { speak("没听清，请重试") }

            // 下面这些暂时不用的方法也得留着，实现接口要求
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
    }

    fun speak(content: String) {
        tts?.speak(content, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun startListening() {
        // 这里只负责启动逻辑，不负责权限申请（权限申请通常交给 Activity）
        speak("请说出您的目的地")
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toString())
            }
            speechRecognizer?.startListening(intent)
        }, 1500)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.CHINESE
    }

    fun destroy() {
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}