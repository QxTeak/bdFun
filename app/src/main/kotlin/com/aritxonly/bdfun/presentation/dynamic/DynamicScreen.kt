package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.aritxonly.bdfun.core.model.feed.DynamicItem
import com.aritxonly.bdfun.core.model.video.VideoItem

@Composable
fun DynamicScreen(
    state: DynamicFeedState, loggedIn: Boolean, onNeedLogin: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit, onOpenDynamic: (DynamicItem) -> Unit, refreshTick: Int = 0,
) {
    val scope = rememberDynamicFeedTaskScope(state)
    val context = LocalContext.current
    val density = LocalDensity.current
    val pull = remember(state) { DynamicPullStateHolder() }
    val request = remember { DynamicRefreshRequestStateHolder(refreshTick) }
    val session = DynamicPullSession(state, pull, with(density) { DynamicPullThreshold.toPx() },
        with(density) { DynamicPullMax.toPx() }, loggedIn)
    DynamicPullEffects(state, pull, session)
    val emotes = LocalRichTextDependencies.current.emotes
    LaunchedEffect(Unit) { emotes.ensure() }
    DynamicRefreshEffect(state, loggedIn, refreshTick, request, session)
    val statusTop = rememberStatusBarTop()
    val connection = remember(state) { session.connection() }
    DynamicPaginationEffects(state, loggedIn)
    val actions = DynamicFeedActions(state, scope, context, loggedIn, onOpenVideo, onOpenDynamic)
    // 查看器放在页面根节点：卡片只交出缩略图矩形，入场/跟手/退场都作用在整页上，
    // 这样拖动关闭时背后的动态列表才会跟着缩到 0.9（Dialog 做不到这一点）。
    val photo = remember { PhotoViewerStateHolder() }
    CompositionLocalProvider(LocalPhotoViewer provides photo) {
        PhotoViewerHost(state = photo, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!loggedIn) {
                    EmptyHint(
                        title = "动态",
                        subtitle = "登录后可查看关注 UP 主的动态",
                        action = "扫码登录",
                        onAction = onNeedLogin,
                    )
                } else {
                    DynamicFeedContent(state, statusTop, pull, connection, actions)
                    DynamicFeedIndicators(state, statusTop)
                }
            }
        }
    }
}
