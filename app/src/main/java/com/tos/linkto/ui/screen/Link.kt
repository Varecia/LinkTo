package com.tos.linkto.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tos.linkto.MainActivity

// 1. 定义数据模型
data class LinkedUser(
    val id: String,
    val name: String,
    val role: String,
    val location: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkScreen(
    isAccessibilityMode: Boolean,          // 新增：接收当前模式状态
    onAccessibilityToggle: (Boolean) -> Unit, // 新增：切换模式的回调
) {
    // 模拟的后台数据
    val mockUsers = remember {
        listOf(
            LinkedUser("1", "张医生", "主治医生", "市中心医院门诊楼 302", "在线"),
            LinkedUser("2", "女儿", "家属", "科技园南区", "离线")
        )
    }

    // 追踪当前选中的用户。如果为 null，说明在列表页；如果不为 null，说明在详情页。
    var selectedUser by remember { mutableStateOf<LinkedUser?>(null) }

    // 追踪弹窗状态
    var showAddDialog by remember { mutableStateOf(false) }

    // 点击时间记录
    var lastClickTimeAdd by remember { mutableLongStateOf(0L) }
    var lastClickTimeBack by remember { mutableLongStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (selectedUser == null) "关联亲友与医生" else selectedUser!!.name)
                },
                // 左侧返回按钮（仅在详情页显示）
                navigationIcon = {
                    if (selectedUser != null) {
                        IconButton(onClick = {
                            if(isAccessibilityMode) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTimeBack < 500) {
                                    selectedUser = null // 双击退回列表
                                } else {
                                    MainActivity.instance.speak("返回联系人列表")
                                }
                                lastClickTimeBack = currentTime
                            }else{
                                selectedUser = null // 双击退回列表
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                },
                // 右侧添加按钮（仅在列表页显示）
                actions = {
                    if (selectedUser == null) {
                        IconButton(onClick = {
                            if(isAccessibilityMode) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTimeAdd < 500) {
                                    showAddDialog = true // 双击打开添加弹窗
                                } else {
                                    MainActivity.instance.speak("添加关联用户")
                                }
                                lastClickTimeAdd = currentTime
                            }else{
                                showAddDialog = true // 双击打开添加弹窗
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (selectedUser == null) {
                // ================= 状态一：用户列表 =================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mockUsers) { user ->
                        UserListItem(
                            user = user,
                            isAccessibilityMode = isAccessibilityMode
                        ) {
                            selectedUser = user
                        }
                    }
                }
            } else {
                // ================= 状态二：用户详情 =================
                UserDetailView(user = selectedUser!!, isAccessibilityMode = isAccessibilityMode)
            }
        }
    }

    // 添加联系人的模拟弹窗
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加关联") },
            text = { Text("未来这里将实现扫描医生/家属的二维码，或输入邀请码的功能。") },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("确定") }
            }
        )
    }
}

// 抽取出来的列表项组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListItem(
    user: LinkedUser,
    isAccessibilityMode: Boolean,
    onSelect: () -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Card(
        onClick = {
            if(isAccessibilityMode) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 500) {
                    onSelect() // 双击进入详情
                } else {
                    MainActivity.instance.speak("${user.name}, ${user.role}, 当前状态${user.status}")
                }
                lastClickTime = currentTime
            }else{
                onSelect() // 进入详情
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${user.role} | ${user.status}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// 抽取出来的详情与留言组件
@Composable
fun UserDetailView(user: LinkedUser, isAccessibilityMode: Boolean) {
    var messageText by remember { mutableStateOf("") }
    var lastClickTimeSend by remember { mutableLongStateOf(0L) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 位置与状态卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("当前位置", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(user.location, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("设备状态: ${user.status}", fontSize = 16.sp)
            }
        }

        // 2. 留言板区域
        Text("发送留言", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = { Text("请输入要发送的内容...") }
        )

        Button(
            onClick = {
                if(isAccessibilityMode) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTimeSend < 500) {
                        if (messageText.isNotBlank()) {
                            MainActivity.instance.speak("留言已发送")
                            messageText = "" // 发送后清空输入框
                        } else {
                            MainActivity.instance.speak("留言内容不能为空")
                        }
                    } else {
                        MainActivity.instance.speak("双击发送留言")
                    }
                    lastClickTimeSend = currentTime
                }else{
                    if (messageText.isNotBlank()) {
                        MainActivity.instance.speak("留言已发送")
                        messageText = "" // 发送后清空输入框
                    } else {
                        MainActivity.instance.speak("留言内容不能为空")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("发送留言", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}