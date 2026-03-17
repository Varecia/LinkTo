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

import com.tos.linkto.ui.components.StupidButton
import com.tos.linkto.ui.components.StupidSurface

@Composable
fun HomeScreen(
    destinationText: String,
    onStartNav: () -> Unit,
    onStartAvoid: () -> Unit,
    isAccessibilityMode: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. 全屏地图占位 (灰底)
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)))

        // 2. 顶部搜索框
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val searchLabel = "搜索框内容：${if (destinationText.isEmpty()) "空" else destinationText}"
            StupidSurface(
                isAccessibilityMode = isAccessibilityMode,
                label = searchLabel,
                onAction = {
                    MainActivity.instance.speak("重新开始语音识别")
                    onStartNav() // 假设这里是你触发语音重试的逻辑
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
            StupidButton(
                isAccessibilityMode = isAccessibilityMode,
                label = "开始导航",
                onAction = { onStartNav() },
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
            StupidButton(
                isAccessibilityMode = isAccessibilityMode,
                label = "开始避障",
                onAction = { onStartAvoid() },
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