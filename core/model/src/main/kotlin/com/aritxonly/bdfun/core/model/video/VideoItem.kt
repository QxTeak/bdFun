package com.aritxonly.bdfun.core.model.video

data class VideoItem(
    val bvid: String,
    val aid: Long,
    val title: String,
    val cover: String,
    val ownerName: String,
    val ownerFace: String,
    val ownerMid: Long,
    val play: Long,
    val danmaku: Long,
    val duration: Int,
    val pubdate: Long,
    val extras: VideoItemExtras = VideoItemExtras(),
) {
    val cid get() = extras.cid
    val reason get() = extras.reason
    val isFollowed get() = extras.isFollowed
    val startPositionMs get() = extras.startPositionMs
    val categoryName get() = extras.categoryName
    val recommendationTrackId get() = extras.recommendationTrackId
    val appRecommendation get() = extras.appRecommendation
    val dislikeReasons get() = extras.dislikeReasons
    val isCharge get() = extras.isCharge
}

/** 列表来源/跳转与推荐扩展；不改变各字段的存储键与默认值。 */
data class VideoItemExtras(
    val cid: Long = 0L,
    val reason: String = "",
    val isFollowed: Boolean = false,
    /** 从「我的评论与弹幕」等入口跳转时，进入视频后直接定位到这个时间点。 */
    val startPositionMs: Long = 0L,
    val categoryName: String = "",
    val recommendationTrackId: String = "",
    val appRecommendation: Boolean = false,
    val dislikeReasons: List<RecommendationReason> = emptyList(),
    /** 包月充电专属视频：列表封面显示黄色「充电」标识。 */
    val isCharge: Boolean = false,
)
