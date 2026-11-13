package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem

@Composable
fun FeedScreen(
    state: FeedState, loggedIn: Boolean, onNeedLogin: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit, onOpenFeed: (FeedItem) -> Unit, refreshTick: Int = 0,
) {
    val scope = rememberFeedTaskScope(state)
    val context = LocalContext.current
    val density = LocalDensity.current
    val pull = remember(state) { FeedPullStateHolder() }
    val request = remember { FeedRefreshRequestStateHolder(refreshTick) }
    val session = FeedPullSession(state, pull, with(density) { FeedPullThreshold.toPx() },
        with(density) { FeedPullMax.toPx() }, loggedIn)
    FeedPullEffects(state, pull, session)
    val emotes = LocalRichTextDependencies.current.emotes
    LaunchedEffect(Unit) { emotes.ensure() }
    FeedRefreshEffect(state, loggedIn, refreshTick, request, session)
    val statusTop = rememberStatusBarTop()
    val connection = remember(state) { session.connection() }
    FeedPaginationEffects(state, loggedIn)
    val actions = FeedActions(state, scope, context, loggedIn, onOpenVideo, onOpenFeed)
    Box(modifier = Modifier.fillMaxSize()) {
        if (!loggedIn) {
            EmptyHint(
                title = "动态",
                subtitle = "登录后可查看关注 UP 主的动态",
                action = "扫码登录",
                onAction = onNeedLogin,
            )
        } else {
            FeedContent(state, statusTop, pull, connection, actions)
            FeedIndicators(state, statusTop)
        }
    }
}
