package com.aritxonly.bdfun.core.model.video

/**
 * 热门（`/x/web-interface/popular`）的一页。
 *
 * [hasMore] 直接取接口的 `no_more` 取反；这一页为空时也视为没有更多，
 * 避免接口异常时页面一直向下请求。
 */
data class PopularPage(
    val videos: List<VideoItem>,
    val hasMore: Boolean,
)
