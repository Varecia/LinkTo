package com.tos.linkto

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tos.linkto.data.repo.SettingsRepo
import com.tos.linkto.ui.screen.AuthScreen
import com.tos.linkto.ui.theme.LinkToTheme

class LoginActivity : ComponentActivity() {

    private lateinit var settingsRepo: SettingsRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()

        settingsRepo = SettingsRepo(this)

        setContent {
            val isDarkTheme by settingsRepo.isDarkTheme.collectAsState()
            val isAccessibilityMode by settingsRepo.isAccessibilityMode.collectAsState()

            LinkToTheme(darkTheme = isDarkTheme){
                AuthScreen(
                    isAccessibilityMode = isAccessibilityMode,
                    onAuthSuccess = {
                        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}