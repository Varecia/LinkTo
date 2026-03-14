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
import com.tos.linkto.vm.AuthVM
import com.tos.linkto.vm.AuthVMFactory

@Composable
fun AuthScreen(
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

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = isLoginMode,
                    onClick = { isLoginMode = true },
                    label = { Text(text=stringResource(R.string.login)) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                FilterChip(
                    selected = !isLoginMode,
                    onClick = { isLoginMode = false },
                    label = { Text(text=stringResource(R.string.register)) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isLoginMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(text=stringResource(R.string.username)) },
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
                label = { Text(text=stringResource(R.string.email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(text=stringResource(R.string.password)) },
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
                    label = { Text(text=stringResource(R.string.check_password)) },
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
                Button(
                    onClick = {
                        if (isLoginMode) {
                            authVM.login(email, password)
                        } else {
                            if (password == confirmPassword) {
                                authVM.register(username, email, password)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = when {
                        isLoginMode -> email.isNotBlank() && password.isNotBlank()
                        else -> username.isNotBlank() && email.isNotBlank() &&
                                password.isNotBlank() && password == confirmPassword
                    }
                ) {
                    Text( text=stringResource(if (isLoginMode) R.string.login else R.string.register))
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
        }
    }
}