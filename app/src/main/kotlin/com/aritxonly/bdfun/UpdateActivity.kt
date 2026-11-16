package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.ActivityPageOptions
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class UpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        setContent {
            TeakActivityScaffold(settings = AppGraph.settings, onBack = { onBackPressedDispatcher.onBackPressed() },
                options = ActivityPageOptions(title = "检查更新")) { UpdateScreenRoute() }
        }
    }
}
