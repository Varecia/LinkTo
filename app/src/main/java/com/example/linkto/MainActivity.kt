package com.example.linkto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 注意：实际开发中建议使用 viewModel() 获取单例，这里为了演示直接实例化
        val viewModel = MainViewModel()

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainContent(vm: MainViewModel) {
    if (vm.currentUser == null) {
        ConfigurationScreen(onSave = { name, phone ->
            // 同步 DataModules 里的 UserProfile 结构
            vm.currentUser = UserProfile(name = name, relativePhone = phone)
            vm.syncProfile()
        })
    } else {
        DashboardScreen(vm)
    }
}

@Composable
fun ConfigurationScreen(onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
        Text("基础信息配置", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("用户姓名") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("紧急联系电话") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if(name.isNotBlank() && phone.isNotBlank()) onSave(name, phone) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存并进入系统")
        }
    }
}

@Composable
fun DashboardScreen(vm: MainViewModel) {
    Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
        Text("系统状态: ${if(vm.isRunning) "运行中" else "待机"}", color = Color.Gray)
        Text("当前用户: ${vm.currentUser?.name}", style = MaterialTheme.typography.headlineSmall)
        // 显示同步的电话号码
        Text("联系电话: ${vm.currentUser?.relativePhone}", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { vm.toggleStreaming() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (vm.isRunning) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (vm.isRunning) "停止计算与同步" else "开启 YOLO+SLAM 链路")
        }

        vm.latestData?.let { data ->
            Card(
                modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📍 融合位置: ${data.fusedLocation}", style = MaterialTheme.typography.bodyLarge)
                    Text("🔍 YOLO 识别: ${data.yoloResult}")
                    Text("📐 SLAM 坐标: ${data.slamPose.joinToString(", ")}")
                    Text("🕒 延迟: ${System.currentTimeMillis() - data.timestamp}ms", color = Color.Gray)
                }
            }
        }
    }
}