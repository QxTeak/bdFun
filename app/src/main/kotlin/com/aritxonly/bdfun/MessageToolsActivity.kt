package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.navigation.readRouteIntent
import com.aritxonly.bdfun.navigation.finishForMissingRoute

class MessageToolsActivity : BdFunActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val route = readRouteIntent(intent) as? Route.MessageTools
        if (route == null) { finishForMissingRoute("消息页面参数"); return }
        setContent { com.aritxonly.bdfun.ui.MessageToolsScreen(route) { finish() } }
    }
}
