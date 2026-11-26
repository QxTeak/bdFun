package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.navigation.readRouteIntent
import com.aritxonly.bdfun.navigation.finishForMissingRoute
import com.aritxonly.bdfun.ui.*

class MessageInboxActivity : BdFunActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val route = readRouteIntent(intent) as? Route.MessageInbox
        if (route == null) { finishForMissingRoute("收件箱参数"); return }
        setContent {
            TeakActivityScaffold(settings = AppGraph.settings, onBack = { finish() },
                options = ActivityPageOptions(title = route.title)) {
                MessageScreen(AppGraph.session.value != null, {}, { finish() }, pageType = route.pageType, title = route.title)
            }
        }
    }
}
