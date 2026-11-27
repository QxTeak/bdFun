package com.aritxonly.bdfun.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class FeedActions(
    private val state: FeedState,
    private val scope: CoroutineScope,
    private val context: Context,
    private val loggedIn: Boolean,
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
    onOpenVideo: (VideoItem) -> Unit,
    onOpenFeed: (FeedItem) -> Unit,
): FeedActions {
    val latestOpenVideo by rememberUpdatedState(onOpenVideo)
    val latestOpenFeed by rememberUpdatedState(onOpenFeed)
    return remember(state, scope, context, loggedIn) {
        FeedActions(
            state = state, scope = scope, context = context, loggedIn = loggedIn,
            onOpenVideo = { latestOpenVideo(it) },
            onOpenFeed = { latestOpenFeed(it) },
        )
    }
}
