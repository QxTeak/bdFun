package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

internal fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.dynamicFeedItems(
    state: FeedState, actions: FeedActions, media: FeedMedia, liveMids: Set<Long>,
) {
    items(state.items, key = { it.id }, contentType = { if (it.video != null) "video" else "dynamic" }) { item ->
        // 逐项订阅点赞状态：别的卡片被点赞时，这张卡的值没变，就不会被带着重组。
        val likedNow by remember(item.id) {
            derivedStateOf { state.liked[item.id] ?: item.liked }
        }
        val countNow by remember(item.id) {
            derivedStateOf { state.likeCounts[item.id] ?: item.like }
        }
        // 每张卡独立形变锚点：省略号从自己的图标长出来，长按则用页面级点锚点。
        val cardMenu = remember(item.id) {
            com.aritxonly.teak.common.navigation.TeakMorphMenuState()
        }
        // 回调按条目记住，只在内容/交互值变化时才新建：参数身份稳定，卡片才能跳过重组。
        val cardActions = remember(item.id, item, actions, media, cardMenu) {
            com.aritxonly.bdfun.ui.FeedCardActions(
                onOpen = {
                    actions.open(item)
                },
                onOpenVideo = { actions.onOpenVideo(it) },
                onLike = { actions.like(item) },
                onComment = { actions.onOpenFeed(item) },
                onRepost = { actions.onOpenFeed(item) },
                onOpenForwarded = { actions.onOpenFeed(it) },
                onToggleMute = media.onToggleMute,
                moreMorphState = cardMenu,
                onMore = { actions.more(item, cardMenu) },
                onLongPressRect = { rect ->
                    actions.longPress(item, androidx.compose.ui.geometry.Offset(rect.left, rect.top))
                },
            )
        }
        // 点赞/计数没变时不要 copy：每次重组都给每张卡 new 两个对象是纯浪费。
        val displayItem = if (likedNow != item.counts.liked || countNow != item.counts.like) {
            item.copy(counts = item.counts.copy(liked = likedNow, like = countNow))
        } else {
            item
        }
        // 只把"这一条要不要自动播放"喂给 presentation：用全局 autoPlayId 当 key 的话，
        // 滚动停下时所有可见卡片的 presentation 都会失效重建。
        val autoPlayForItem = media.autoPlay && media.autoPlayId == item.id
        val presentation = remember(item.id, likedNow, autoPlayForItem, media.muted) {
            com.aritxonly.bdfun.ui.FeedCardPresentation(
                liked = likedNow,
                autoPlay = autoPlayForItem,
                muted = media.muted,
            )
        }
        FeedCard(
            item = displayItem,
            actions = cardActions,
            presentation = presentation,
            live = liveMids.contains(item.mid),
        )
    }
}
