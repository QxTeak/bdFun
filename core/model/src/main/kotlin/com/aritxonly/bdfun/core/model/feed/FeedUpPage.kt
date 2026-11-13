package com.aritxonly.bdfun.core.model.feed

data class FeedUpPage(val items: List<FeedAuthor>, val offset: String, val hasMore: Boolean)
