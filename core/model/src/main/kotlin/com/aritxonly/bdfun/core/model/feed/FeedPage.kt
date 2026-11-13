package com.aritxonly.bdfun.core.model.feed

data class FeedPage(
    val items: List<FeedItem>,
    val offset: String,
    val hasMore: Boolean,
)
