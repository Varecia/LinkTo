package com.tos.linkto.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tos.linkto.R
import com.tos.linkto.ui.components.StupidButton
import com.tos.linkto.ui.components.StupidFilterChip
import com.tos.linkto.vm.AuthVM
import com.tos.linkto.vm.AuthVMFactory

@Composable
fun AuthScreen(
    isAccessibilityMode: Boolean, // 接收无障碍模式状态
    onAuthSuccess: () -> Unit,
    authVM: AuthVM = viewModel(factory = AuthVMFactory())
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val uiState by authVM.uiState.collectAsState()
    val currentUser by authVM.currentUser.collectAsState()

    LaunchedEffect(uiState.isSuccess, currentUser) {
        if (uiState.isSuccess && currentUser != null) {
            onAuthSuccess()
            authVM.resetState()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 模式切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                StupidFilterChip(
                    isAccessibilityMode = isAccessibilityMode,
                    label = "切换至${stringResource(R.string.login)}模式",
                    selected = isLoginMode,
                    onAction = { isLoginMode = true },
                    chipLabel = { Text(text = stringResource(R.string.login)) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                StupidFilterChip(
                    isAccessibilityMode = isAccessibilityMode,
                    label = "切换至${stringResource(R.string.register)}模式",
                    selected = !isLoginMode,
                    onAction = { isLoginMode = false },
                    chipLabel = { Text(text = stringResource(R.string.register)) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 输入框保持不变（系统输入法自带了很好的无障碍读屏功能）
            if (!isLoginMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(text = stringResource(R.string.username)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isLoginMode && username.isBlank()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(text = stringResource(R.string.email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(text = stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(text = stringResource(R.string.check_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    isError = password != confirmPassword
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                // 主操作按钮
                val actionText = stringResource(if (isLoginMode) R.string.login else R.string.register)
                StupidButton(
                    isAccessibilityMode = isAccessibilityMode,
                    label = "$actionText",
                    enabled = when {
                        isLoginMode -> email.isNotBlank() && password.isNotBlank()
                        else -> username.isNotBlank() && email.isNotBlank() &&
                                password.isNotBlank() && password == confirmPassword
                    },
                    onAction = {
                        if (isLoginMode) {
                            authVM.login(email, password)
                        } else {
                            if (password == confirmPassword) {
                                authVM.register(username, email, password)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = actionText)
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }
    }
}