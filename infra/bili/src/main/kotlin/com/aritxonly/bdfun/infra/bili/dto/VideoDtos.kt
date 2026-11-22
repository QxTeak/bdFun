package com.aritxonly.bdfun.infra.bili.dto

/** 只保存接口原始值；图片地址、HTML 与业务模型转换留给 mapper。 */
internal data class VideoDetailDto(
    val bvid: String, val aid: Long, val title: String, val cover: String,
    val description: String, val owner: VideoOwnerDto, val stats: VideoStatsDto,
    val timing: VideoTimingDto, val category: String, val collection: VideoCollectionDto,
    val interaction: VideoInteractionDto, val copyright: Int = 0,
)
internal data class VideoOwnerDto(val name: String, val face: String, val mid: Long)
internal data class VideoStatsDto(
    val play: Long, val danmaku: Long, val like: Long, val coin: Long,
    val favorite: Long, val share: Long, val reply: Long,
)
internal data class VideoTimingDto(val publishedAt: Long, val duration: Int, val cid: Long)
internal data class VideoPageDto(val cid: Long, val page: Int, val part: String, val duration: Int)
internal data class VideoEpisodeDto(
    val bvid: String, val aid: Long, val cid: Long, val title: String, val cover: String,
    val duration: Int, val play: Long, val danmaku: Long, val publishedAt: Long,
)
internal data class VideoCollectionDto(
    val pages: List<VideoPageDto>, val episodes: List<VideoEpisodeDto>, val title: String,
)
internal data class VideoInteractionDto(val liked: Boolean, val coined: Boolean, val favorited: Boolean)
internal data class VideoFeedDto(
    val bvid: String, val aid: Long, val title: String, val cover: String,
    val owner: VideoOwnerDto, val stats: VideoStatsDto, val timing: VideoTimingDto,
    val category: String, val trackId: String, val reason: String, val followed: Boolean,
    val isCharge: Boolean = false,
)
