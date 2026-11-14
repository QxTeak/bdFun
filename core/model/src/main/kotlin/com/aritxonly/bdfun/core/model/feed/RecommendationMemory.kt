package com.aritxonly.bdfun.core.model.feed

/** 按视频聚合证据，避免一次点击、观看被重复算成多个独立样本。 */
data class RecommendationMemory(
    val bvid: String, val owner: Long, val category: String,
    val updatedAt: Long, val exposedAt: Long = 0, val clickedAt: Long = 0,
    val satisfaction: Double = 0.0, val watchedAt: Long = 0,
)

data class RecommendationArchive(
    val enabled: Boolean = false,
    val accounts: Map<String, List<RecommendationMemory>> = emptyMap(),
)
