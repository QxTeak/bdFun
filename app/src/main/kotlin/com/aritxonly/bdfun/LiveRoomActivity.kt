package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class LiveRoomActivity : BdFunActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        AppGraph.ensure(applicationContext)
        val requestedRoomId = intent.getLongExtra(EXTRA_ROOM_ID, 0L)
        setContent { LiveRoomTheme(AppGraph.settings) { LiveRoomRoute(this, requestedRoomId) } }
    }
    companion object { const val EXTRA_ROOM_ID = "room_id" }
}
