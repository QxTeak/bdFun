package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToMessageFeed
import com.aritxonly.bdfun.navigateToLogin
import android.content.Context
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.ui.MessageScreen

@Composable
internal fun MessageScreenRoute(context: Context) {
    val navigation = com.aritxonly.bdfun.ui.LocalMessageNavigation.current
    MessageScreen(
        loggedIn = AppGraph.session.value != null,
        onNeedLogin = { navigateToLogin(context) },
        onBack = { navigation?.back?.invoke() ?: (context as? android.app.Activity)?.finish() },
        onOpenReplies = { navigation?.feed?.invoke(com.aritxonly.bdfun.ui.MessageFeedKind.Reply, 0) ?: navigateToMessageFeed(context, "reply") },
        onOpenLikes = { navigation?.feed?.invoke(com.aritxonly.bdfun.ui.MessageFeedKind.Like, 0) ?: navigateToMessageFeed(context, "like") },
        onOpenFans = { navigation?.feed?.invoke(com.aritxonly.bdfun.ui.MessageFeedKind.Fans, 0) ?: navigateToMessageFeed(context, "fans") },
    )
}
