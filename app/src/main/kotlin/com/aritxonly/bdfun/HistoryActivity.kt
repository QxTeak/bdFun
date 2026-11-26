package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.LibraryPageShell

class HistoryActivity : BdFunActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val settings = AppGraph.settings
        setContent {
            com.aritxonly.teak.common.navigation.TeakMorphMenuProvider {
                LibraryPageShell(
                    context = this,
                    settings = settings,
                    title = "历史",
                    fixedMode = 1,
                    onBack = { finish() },
                )
            }
        }
    }
}
