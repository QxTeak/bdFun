package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.ui.ActivityPageOptions
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.CacheScreen
import com.aritxonly.bdfun.ui.TeakActivityScaffold

/** 离线缓存列表。 */
class CacheActivity : BdFunActivity() {
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
                    title = "离线缓存",
                ),
            ) {
                CacheScreen(AppGraph.videoCache) { navigateToVideo(this@CacheActivity, it) }
            }
        }
    }
}
