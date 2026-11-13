package com.aritxonly.bdfun.ui

import android.content.Context
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
