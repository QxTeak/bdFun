package com.aritxonly.bdfun.core.model.feed

data class FeedLiveRecommendation(
    val roomId: Long, val title: String, val cover: String,
    val live: Boolean, val areaName: String, val watched: String,
)
