package com.aritxonly.bdfun.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 动态卡片的形变菜单锚点。长按用一份页面级的点锚点（从手指处长出来），
 * 省略号用每张卡自己的锚点（从图标长出来），两者共用同一个宿主。
 */
@Stable
internal class FeedCardMenuUi {
    val pointMenu = TeakMorphMenuState()
    var point by mutableStateOf(Offset.Zero)
    /** 长按后等两帧再展开，Miuix 的形变才有起点可以插值。 */
    var pendingOpen by mutableStateOf(false)
}

internal class FeedActions(
    private val state: FeedState,
    private val scope: CoroutineScope,
    private val context: Context,
    private val loggedIn: Boolean,
    private val menu: FeedCardMenuUi,
    val onOpenVideo: (VideoItem) -> Unit,
    val onOpenFeed: (FeedItem) -> Unit,
) {
    fun open(item: FeedItem) { state.markSeen(item.mid); onOpenFeed(item) }
    fun like(item: FeedItem) {
        scope.launch {
            when (val result = state.toggleLike(item)) {
                is Outcome.Success -> context.showToast(if (result.value) "点赞成功" else "已取消赞")
                is Outcome.Failure -> context.showToast("点赞失败")
                null -> Unit
            }
        }
    }
    fun loadMore() { scope.launch { state.loadMore(loggedIn) } }
    fun retry() { scope.launch { state.retry(loggedIn) } }

    /** 省略号：记录当前条目，形变锚点自己负责展开。 */
    fun more(item: FeedItem, menuState: TeakMorphMenuState) {
        state.menuItem = item
        state.menuState = menuState
    }

    /** 长按卡片：从手指位置长出菜单，不画省略号副本。 */
    fun longPress(item: FeedItem, point: Offset) {
        menu.point = point
        state.menuItem = item
        state.menuState = menu.pointMenu
        menu.pendingOpen = true
    }
}

/**
 * 固定 FeedActions 的实例：它内部持有整页状态和回调，每次重组都 new 一个会让
 * 列表里所有 `remember(..., actions, ...)` 失效、整屏卡片跟着重组。
 * 外部回调按最新值转发，实例只在 state/scope/context/登录态变化时重建。
 */
@Composable
internal fun rememberFeedActions(
    state: FeedState,
    scope: CoroutineScope,
    context: Context,
    loggedIn: Boolean,
    menu: FeedCardMenuUi,
    onOpenVideo: (VideoItem) -> Unit,
    onOpenFeed: (FeedItem) -> Unit,
): FeedActions {
    val latestOpenVideo by rememberUpdatedState(onOpenVideo)
    val latestOpenFeed by rememberUpdatedState(onOpenFeed)
    return remember(state, scope, context, loggedIn, menu) {
        FeedActions(
            state = state, scope = scope, context = context, loggedIn = loggedIn, menu = menu,
            onOpenVideo = { latestOpenVideo(it) },
            onOpenFeed = { latestOpenFeed(it) },
        )
    }
}
