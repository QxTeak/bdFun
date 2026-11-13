package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items

internal fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.dynamicFeedItems(
    state: FeedState, actions: FeedActions, media: FeedMedia,
) {
    items(state.items, key = { it.id }, contentType = { if (it.video != null) "video" else "dynamic" }) { item ->
        val likedNow = state.liked[item.id] ?: item.liked
        val countNow = state.likeCounts[item.id] ?: item.like
        FeedCard(
            item = item.copy(
                counts = item.counts.copy(
                    liked = likedNow,
                    like = countNow,
                ),
            ),
            actions = com.aritxonly.bdfun.ui.FeedCardActions(
                onOpen = {
                actions.open(item)
            },
                onOpenVideo = { actions.onOpenVideo(it) },
                onLike = { actions.like(item) },
                onComment = { actions.onOpenFeed(item) },
                onRepost = { actions.onOpenFeed(item) },
                onOpenForwarded = { actions.onOpenFeed(it) },
                onToggleMute = media.onToggleMute
            ),
            presentation = com.aritxonly.bdfun.ui.FeedCardPresentation(liked = likedNow, autoPlay = media.autoPlay && media.autoPlayId == item.id, muted = media.muted),
        )
    }

}
