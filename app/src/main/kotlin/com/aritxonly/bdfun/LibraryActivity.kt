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
            val morphMenu = androidx.compose.runtime.remember {
                com.aritxonly.teak.common.navigation.TeakMorphMenuController()
            }
            androidx.compose.runtime.CompositionLocalProvider(
                com.aritxonly.teak.common.navigation.LocalTeakMorphMenu provides morphMenu,
            ) {
                androidx.compose.foundation.layout.Box {
                    LibraryPageShell(
                        context = this@LibraryActivity,
                        settings = settings,
                        title = "收藏",
                        fixedMode = 0,
                        onBack = { finish() },
                    )
                    com.aritxonly.teak.common.navigation.TeakMorphMenuOverlay(morphMenu)
                }
            }
        }
    }
}
