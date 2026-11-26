package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.ActivityPageOptions

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.DonationScreen
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class DonationActivity : BdFunActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val settings = AppGraph.settings
        setContent {
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                options = ActivityPageOptions(
                    title = "捐赠",
                ),
            ) {
                DonationScreen()
            }
        }
    }
}
