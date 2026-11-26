package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.ActivityPageOptions

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.LoginScreen
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class LoginActivity : BdFunActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val login = AppGraph.createQrLogin()
        val settings = AppGraph.settings
        setContent {
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                options = ActivityPageOptions(
                    showBackButton = false,
                ),
            ) {
                LoginScreen(
                    controller = login,
                    onBack = { finish() },
                    onSuccess = { profile ->
                        // 登录态写进进程级单例，MainActivity 的 Compose 状态会立即刷新
                        AppGraph.session.value = profile
                        finish()
                    },
                )
            }
        }
    }
}
