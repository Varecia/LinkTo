package com.tos.linkto.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tos.linkto.MainActivity
import com.tos.linkto.R
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    tts: TextToSpeech?,
    destinationText: String,
    onStartNav: () -> Unit,
    onStartAvoid: () -> Unit,
    isAccessibilityMode: Boolean,          // 新增：接收当前模式状态
    onAccessibilityToggle: (Boolean) -> Unit, // 新增：切换模式的回调
) {
    // 记录点击时间的状态
    var lastClickTimeSearch by remember { mutableLongStateOf(0L) }
    var lastClickTimeNav by remember { mutableLongStateOf(0L) }
    var lastClickTimeAvoid by remember { mutableLongStateOf(0L) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. 全屏地图占位 (灰底)
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)))

        // 2. 顶部搜索框
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val searchLabel = "搜索框内容：${if (destinationText.isEmpty()) "空" else destinationText}"
            Surface(
                onClick = {
                    if(isAccessibilityMode) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTimeSearch < 500) {
                            // 双击搜索框逻辑：清除内容或重新启动语音
                            MainActivity.instance.speak("重新开始语音识别")
                            onStartNav()
                        } else {
                            MainActivity.instance.speak(searchLabel)
                        }
                        lastClickTimeSearch = currentTime
                    }else{
                        MainActivity.instance.speak("重新开始语音识别")
                        onStartNav()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                color = Color(0xFF333333),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (destinationText.isEmpty()) "等待语音输入目的地..." else destinationText,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // 3. 底部两个巨型按钮
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
        ) {
            // 开始导航按钮
            Button(
                onClick = {
                    if(isAccessibilityMode) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTimeNav < 500) {
                            onStartNav() // 执行导航
                        } else {
                            MainActivity.instance.speak("开始导航")
                        }
                        lastClickTimeNav = currentTime
                    }else{
                        onStartNav() // 执行导航
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(bottom = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = MaterialTheme.shapes.large
            ) {
                Text("开始导航", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            // 开始避障按钮
            Button(
                onClick = {
                    if(isAccessibilityMode) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTimeAvoid < 500) {
                            onStartAvoid() // 执行避障
                        } else {
                            MainActivity.instance.speak("开始避障")
                        }
                        lastClickTimeAvoid = currentTime
                    }else{
                        onStartAvoid() // 执行避障
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = MaterialTheme.shapes.large
            ) {
                Text("开始避障", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}