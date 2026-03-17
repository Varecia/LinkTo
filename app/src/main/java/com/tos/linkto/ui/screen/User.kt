package com.tos.linkto.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.foundation.layout.*
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

import com.tos.linkto.ui.components.StupidCard
import com.tos.linkto.ui.components.StupidButton
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class) // 使用可点击 Card 需要
@Composable
fun UserScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    isAccessibilityMode: Boolean,
    onAccessibilityToggle: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var timerCount by remember { mutableIntStateOf(5) } // 倒计时 5 秒

    // 自动取消计时器逻辑
    if (showLogoutDialog) {
        LaunchedEffect(key1 = showLogoutDialog) {
            //立即朗读弹窗
            if (isAccessibilityMode) {
                MainActivity.instance.speak("确认退出吗？十秒内无操作将自动取消。")
            }

            timerCount = 10
            while (timerCount > 0) {
                delay(1000) // 每秒减一
                timerCount--
            }
            showLogoutDialog = false // 倒计时结束，自动关闭
        }
    }

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
        StupidCard(
            isAccessibilityMode = isAccessibilityMode,
            label = modeLabel,
            onAction = { onAccessibilityToggle(!isAccessibilityMode) },
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

        StupidCard(
            isAccessibilityMode = isAccessibilityMode,
            label = themeLabel,
            onAction = { onThemeToggle() },
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
        StupidButton(
            isAccessibilityMode = isAccessibilityMode,
            label = "退出登录",
            onAction = { showLogoutDialog = true },
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

    // ================= 确认退出弹窗 =================
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("确认退出") },
            text = {
                Text("您确定要退出登录吗？\n该操作将在 ${timerCount} 秒后自动取消。")
            },
            confirmButton = {
                // 确认按钮也得是 StupidButton，方便盲人双击确认
                StupidButton(
                    isAccessibilityMode = isAccessibilityMode,
                    label = "确认退出",
                    onAction = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}