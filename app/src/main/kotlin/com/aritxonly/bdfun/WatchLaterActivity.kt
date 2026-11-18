package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.ActivityPageOptions
import com.aritxonly.bdfun.ui.ActivityPageSlots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.ui.LibraryBatchOverlay
import com.aritxonly.bdfun.ui.LibraryMorphMenuContent
import com.aritxonly.bdfun.ui.LibraryTopActions
import com.aritxonly.bdfun.ui.TeakActivityScaffold
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import com.aritxonly.teak.common.navigation.TeakMorphMenuState

class WatchLaterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val settings = AppGraph.settings
        setContent {
            val menuState = remember { TeakMorphMenuState() }
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                options = ActivityPageOptions(
                    title = "稍后再看",
                    limitContentWidth = false,
                ),
                slots = ActivityPageSlots(
                    topBarActions = { LibraryTopActions(menuState) },
                    overlay = {
                    LibraryBatchOverlay()
                },
                    menuOverlay = {
                    TeakMorphMenuHost(menuState) { dismiss ->
                        LibraryMorphMenuContent(dismiss)
                    }
                },
                ),
            ) {
                LibraryScreenRoute(this, 2, "稍后再看")
            }
        }
    }
}
