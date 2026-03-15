package com.tos.linkto

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tos.linkto.ui.screen.AuthScreen
import com.tos.linkto.ui.theme.LinkToTheme

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()

        setContent {
            LinkToTheme {
                AuthScreen(
                    onAuthSuccess = {
                        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}