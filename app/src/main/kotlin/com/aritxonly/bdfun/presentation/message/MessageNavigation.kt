package com.aritxonly.bdfun.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.aritxonly.bdfun.core.model.message.MessageSession
import com.aritxonly.bdfun.core.navigation.Route

/** 独立 Activity 沿用外部路由，消息中心宿主将子页派发到当前窗口。 */
internal data class MessageNavigation(
    val route: (Route) -> Unit,
    val feed: (MessageFeedKind, Int) -> Unit,
    val back: () -> Unit,
    val selectedSessionId: String? = null,
)

internal val LocalMessageNavigation = staticCompositionLocalOf<MessageNavigation?> { null }

internal fun MessageSession.chatRoute() = Route.PrivateChat(
    talkerId, sessionType, name, avatar, unread, maxSeqno,
)

@androidx.compose.runtime.Composable
internal fun messageRouteAction(): (Route) -> Unit {
    val navigation = LocalMessageNavigation.current
    val context = androidx.compose.ui.platform.LocalContext.current
    return navigation?.route ?: { route ->
        context.startActivity(com.aritxonly.bdfun.navigation.createRouteIntent(context, route))
    }
}
