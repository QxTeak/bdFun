package com.aritxonly.bdfun.core.model.video

data class VideoDetail(
    val bvid: String,
    val aid: Long,
    val title: String,
    val cover: String,
    val desc: String,
    val owner: VideoDetailOwner,
    val counts: VideoDetailCounts,
    val info: VideoDetailInfo,
    val interaction: VideoDetailInteraction = VideoDetailInteraction(),
) {
    val ownerName get() = owner.ownerName
    val ownerFace get() = owner.ownerFace
    val ownerMid get() = owner.ownerMid
    val ownerSign get() = owner.ownerSign
    val play get() = counts.play
    val danmaku get() = counts.danmaku
    val like get() = counts.like
    val coin get() = counts.coin
    val favorite get() = counts.favorite
    val share get() = counts.share
    val reply get() = counts.reply
    val pubdate get() = info.pubdate
    val duration get() = info.duration
    val cid get() = info.cid
    val tname get() = info.tname
    val pages get() = info.pages
    val episodes get() = info.episodes
    val seasonTitle get() = info.seasonTitle
    /** 1 = 原创，2 = 转载；接口缺失时为 0。 */
    val copyright get() = info.copyright
    val isFollowed get() = interaction.isFollowed
    val liked get() = interaction.liked
    val coined get() = interaction.coined
    val favorited get() = interaction.favorited
}

data class VideoDetailOwner(
    val ownerName: String,
    val ownerFace: String,
    val ownerMid: Long,
    val ownerSign: String,
)

data class VideoDetailCounts(
    val play: Long,
    val danmaku: Long,
    val like: Long,
    val coin: Long,
    val favorite: Long,
    val share: Long,
    val reply: Long,
)

data class VideoDetailInfo(
    val pubdate: Long,
    val duration: Int,
    val cid: Long,
    val tname: String,
    val pages: List<VideoPage>,
    /** 该视频所属 UGC 合集的全部单集（没有合集时为空）。 */
    val episodes: List<VideoEpisode> = emptyList(),
    /** UGC 合集标题（没有合集时为空）。 */
    val seasonTitle: String = "",
    /** 1 = 原创，2 = 转载；老缓存/缺失时为 0。 */
    val copyright: Int = 0,
)

data class VideoDetailInteraction(
    val isFollowed: Boolean = false,
    val liked: Boolean = false,
    val coined: Boolean = false,
    val favorited: Boolean = false,
)
