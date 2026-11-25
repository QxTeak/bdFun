package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.LibraryPageShell

class LibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val settings = AppGraph.settings
        setContent {
            com.aritxonly.teak.common.navigation.TeakMorphMenuProvider {
                LibraryPageShell(
                    context = this@LibraryActivity,
                    settings = settings,
                    title = "收藏",
                    fixedMode = 0,
                    onBack = { finish() },
                )
            }
        }
    }
}
