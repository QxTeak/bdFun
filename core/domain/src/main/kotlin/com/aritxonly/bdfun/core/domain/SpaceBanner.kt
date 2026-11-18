package com.aritxonly.bdfun.core.domain

fun normalizeSpaceBanner(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
    url.removePrefix("/").startsWith("bfs/") -> "https://i0.hdslb.com/${url.removePrefix("/")}"
    else -> url
}

/**
 * 空间头图候选顺序：App 空间装扮图（imgUrl / night_imgurl）优先，网页版 `top_photo` 最后兜底。
 *
 * `top_photo` 常见 3009×313 这种超宽图，裁进 16:10 的通栏头图只会显示图片的一角；
 * 收藏集装扮的 cover 则是 2:3 竖长原画，同样不能当通栏头图。两者都不应排在 App 装扮图前面，
 * 与官方 App 的空间页保持一致。
 */
fun orderedSpaceBanners(web: String, day: String, night: String): List<String> =
    listOf(day, night, web)
        .filter { it.isNotBlank() }
        .map(::normalizeSpaceBanner)
        .distinct()
