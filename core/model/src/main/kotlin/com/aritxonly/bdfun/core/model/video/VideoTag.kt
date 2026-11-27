package com.aritxonly.bdfun.core.model.video

/**
 * 视频详情里的标签胶囊。
 *
 * 字段来自 `/x/web-interface/view/detail/tag`（PiliPlus 同款）：tag_type 常见
 * `topic`（话题）/`bgm`（BGM）/其它普通标签，展示时只按类型加前缀，不猜新字段。
 */
data class VideoTag(
    val id: Long = 0L,
    val name: String,
    val type: String = "",
    val musicId: String = "",
)
