package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import com.aritxonly.bdfun.core.model.feed.FeedItem

internal fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.dynamicFeedItems(
    state: FeedState, feedItems: List<FeedItem>, actions: FeedActions, media: FeedMedia,
    liveMids: Set<Long>,
) {
    items(feedItems, key = { it.id }, contentType = { if (it.video != null) "video" else "dynamic" }) { item ->
        // 逐项订阅点赞状态：别的卡片被点赞时，这张卡的值没变，就不会被带着重组。
        val likedNow by remember(state, item.id, item.liked) {
            derivedStateOf(structuralEqualityPolicy()) { state.liked[item.id] ?: item.liked }
        }
        val countNow by remember(state, item.id, item.like) {
            derivedStateOf(structuralEqualityPolicy()) { state.likeCounts[item.id] ?: item.like }
        }
        // 每张卡独立形变锚点：省略号从自己的图标长出来，长按则用页面级点锚点。
        val cardMenu = remember(item.id) {
            com.aritxonly.teak.common.navigation.TeakMorphMenuState()
        }
        // 回调按条目记住，只在内容/交互值变化时才新建：参数身份稳定，卡片才能跳过重组。
        val cardActions = remember(item, actions, media.onToggleMute, cardMenu) {
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
        val displayItem = remember(item, likedNow, countNow) {
            if (likedNow != item.counts.liked || countNow != item.counts.like) {
                item.copy(counts = item.counts.copy(liked = likedNow, like = countNow))
            } else {
                item
            }
        }
        // 播放目标切换时，只让播放资格变化的卡片重组。
        val autoPlayForItem by remember(media.autoPlay, media.autoPlayId, item.id) {
            derivedStateOf(structuralEqualityPolicy()) { media.autoPlay && media.autoPlayId.value == item.id }
        }
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
