package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem

internal data class FeedCardActions(
    val onOpen: () -> Unit,
    val onOpenVideo: (VideoItem) -> Unit,
    val onLike: () -> Unit,
    val onComment: () -> Unit,
    val onRepost: () -> Unit,
    val onOpenForwarded: (FeedItem) -> Unit = {},
    val onToggleMute: () -> Unit = {},
)

internal data class FeedCardPresentation(
    val liked: Boolean,
    val autoPlay: Boolean = false,
    val muted: Boolean = true,
    val expanded: Boolean = false,
    val animatedOpen: Boolean = true,
)
