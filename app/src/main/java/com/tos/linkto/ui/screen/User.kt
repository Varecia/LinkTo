package com.tos.linkto.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tos.linkto.LoginActivity
import com.tos.linkto.MainActivity
import com.tos.linkto.R
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class) // 使用可点击 Card 需要
@Composable
fun UserScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    isAccessibilityMode: Boolean,          // 新增：接收当前模式状态
    onAccessibilityToggle: (Boolean) -> Unit, // 新增：切换模式的回调
    onLogout: () -> Unit
) {
    // 记录点击时间的本地状态
    var lastClickTimeTheme by remember { mutableLongStateOf(0L) }
    var lastClickTimeLogout by remember { mutableLongStateOf(0L) }
    var lastClickTimeMode by remember { mutableLongStateOf(0L) } // 模式开关的点击时间

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "设置中心",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // ================= 1. 无障碍语音模式开关 =================
        val modeLabel = if (isAccessibilityMode) "无障碍语音模式已开启，双击关闭" else "无障碍语音模式已关闭"
        Card(
            onClick = {
                if (isAccessibilityMode) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTimeMode < 500) {
                        onAccessibilityToggle(false) // 双击关闭
                        MainActivity.instance.speak("无障碍模式已关闭")
                    } else {
                        MainActivity.instance.speak(modeLabel)
                    }
                    lastClickTimeMode = currentTime
                } else {
                    // 如果当前是关闭状态，直接单击就能开启
                    onAccessibilityToggle(true)
                    MainActivity.instance.speak("无障碍模式已开启")
                }
            },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("无障碍语音提示", fontSize = 20.sp)
                }
                Switch(
                    checked = isAccessibilityMode,
                    onCheckedChange = null
                )
            }
        }

        // --- 主题切换卡片 ---
        val themeLabel = if (isDarkTheme) "当前是深色模式" else "当前是浅色模式"

        Card(
            onClick = {
                if(isAccessibilityMode) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTimeTheme < 500) {
                        onThemeToggle() // 执行切换
                    } else {
                        MainActivity.instance.speak(themeLabel) // 单击朗读状态
                    }
                    lastClickTimeTheme = currentTime
                }else{
                    onThemeToggle() // 执行切换
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("主题模式", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(
                    imageVector = Icons.Default.Brightness4,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- 退出登录按钮 ---
        Button(
            onClick = {
                if(isAccessibilityMode) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTimeLogout < 500) {
                        onLogout() // 执行退出
                    } else {
                        MainActivity.instance.speak("退出登录") // 单击朗读名称
                    }
                    lastClickTimeLogout = currentTime
                }else{
                    onLogout()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("退出登录", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}