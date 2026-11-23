package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.remember

internal fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.dynamicFeedItems(
    state: FeedState, actions: FeedActions, media: FeedMedia,
) {
    // 直播 UP 集合算一次查表：不再让每张卡片各自扫一遍列表。
    val liveMids = state.liveUsers.mapTo(HashSet()) { it.mid }
    items(state.items, key = { it.id }, contentType = { if (it.video != null) "video" else "dynamic" }) { item ->
        val likedNow = state.liked[item.id] ?: item.liked
        val countNow = state.likeCounts[item.id] ?: item.like
        // 回调按条目记住，只在内容/交互值变化时才新建：参数身份稳定，卡片才能跳过重组。
        val cardActions = remember(item.id, item, actions, media.onToggleMute) {
            com.aritxonly.bdfun.ui.FeedCardActions(
                onOpen = {
                actions.open(item)
            },
                onOpenVideo = { actions.onOpenVideo(it) },
                onLike = { actions.like(item) },
                onComment = { actions.onOpenFeed(item) },
                onRepost = { actions.onOpenFeed(item) },
                onOpenForwarded = { actions.onOpenFeed(it) },
                onToggleMute = media.onToggleMute
            )
        }
        FeedCard(
            item = item.copy(
                counts = item.counts.copy(
                    liked = likedNow,
                    like = countNow,
                ),
            ),
            actions = cardActions,
            presentation = com.aritxonly.bdfun.ui.FeedCardPresentation(liked = likedNow, autoPlay = media.autoPlay && media.autoPlayId == item.id, muted = media.muted),
            live = liveMids.contains(item.mid),
        )
    }

}
