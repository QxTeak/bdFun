package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.ActivityPageOptions
import com.aritxonly.bdfun.ui.ActivityPageSlots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.ui.LibraryBatchOverlay
import com.aritxonly.bdfun.ui.LibraryMorphMenuContent
import com.aritxonly.bdfun.ui.LibraryTopActions
import com.aritxonly.bdfun.ui.TeakActivityScaffold
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import com.aritxonly.teak.common.navigation.TeakMorphMenuState

class LibraryActivity : ComponentActivity() {
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
                    title = "收藏",
                    // 宽屏交给网格按首页的 280..560dp 卡片自动加列，不再压成单栏 680dp。
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
                LibraryScreenRoute(this)
            }
        }
    }
}
