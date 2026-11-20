package com.aritxonly.bdfun.core.model.video

/** 合集里的单集（UGC `ugc_season` 或 PGC 番剧/影视的一集），可能属于另一个 bvid。 */
data class VideoEpisode(
    val bvid: String,
    val aid: Long,
    val cid: Long,
    val title: String,
    val cover: String,
    val duration: Int,
    val play: Long = 0L,
    val danmaku: Long = 0L,
    val pubdate: Long = 0L,
    /** PGC 剧集的 ep_id；UGC 合集单集为 0。换集与评论区都以它为准。 */
    val epId: Long = 0L,
)
