package com.tos.linkto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.tos.linkto.data.repo.AuthRepo
import com.tos.linkto.ui.theme.LinkToTheme
import com.tos.linkto.ui.screen.HomeScreen
import com.tos.linkto.ui.screen.LinkScreen
import com.tos.linkto.ui.screen.UserScreen

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var instance: MainActivity
            private set
    }

    lateinit var authRepo: AuthRepo
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        instance = this
        authRepo = AuthRepo(this)

        setContent {
            LinkToTheme {
                LinkToApp()
            }
        }
    }

    fun <T : Activity> raiseActivity(target: Class<T>) {
        val intent = Intent(this, target)
        startActivity(intent)
    }
}

@PreviewScreenSizes
@Composable
fun LinkToApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> HomeScreen()
            AppDestinations.LINK -> LinkScreen()
            AppDestinations.USER -> UserScreen()
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("首页", R.drawable.ic_home),
    LINK("关联", R.drawable.ic_favorite),
    USER("我的", R.drawable.ic_account_box),
}
