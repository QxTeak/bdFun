package com.aritxonly.bdfun.core.model.video

data class VideoPage(
    val cid: Long,
    val page: Int,
    val part: String,
    val duration: Int,
    /** 该 P 的首帧封面（B 站 `pages[].first_frame`）；空串表示接口没给，展示时回退整稿封面。 */
    val firstFrame: String = "",
)
