package com.aritxonly.bdfun.core.common

/** 沿用旧图片 URL 规则；已带裁切参数的链接不能再追加尺寸。 */
fun biliImageUrl(url: String, width: Int = 0): String {
    if (url.isBlank()) return ""
    var result = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
        else -> url
    }
    // GIF 不能被服务端转成静态 webp，否则动图只剩第一帧；原样交给 Coil 的动图解码器。
    if (width > 0 && !result.contains("@") && !result.isGifImage()) {
        // B 站图片的 @尺寸 必须放在路径末尾、查询串之前；直接拼在 ? 后面会得到 404。
        val query = result.indexOf('?').takeIf { it >= 0 }
        val base = if (query != null) result.substring(0, query) else result
        val suffix = if (query != null) result.substring(query) else ""
        result = "$base@${width}w_${(width * 9 / 16)}h_1c.webp$suffix"
    }
    return result
}

/** 判断 B 站图片是不是 GIF（路径以 .gif 结尾，查询串和片段不算）。 */
private fun String.isGifImage(): Boolean {
    val path = substringBefore('?').substringBefore('#')
    return path.endsWith(".gif", ignoreCase = true)
}

/**
 * 只按宽度缩放的缩略图地址（`@720w.webp`），**不裁切**。
 *
 * 列表缩略图和图片查看器必须来自同一张"未裁切"的源图：
 * 如果列表先用 `_1c` 让服务端裁成 16:9、再由单元格裁一次，
 * 查看器按原图只裁一次，两边看到的画面就不一致——
 * 只有本来就是 16:9 的图才碰巧对得上，其他比例收放都会错位。
 *
 * 这里保留原始宽高比，两边的"中心裁切"结果就完全一致，
 * 入场/退场的共用元素动画才能严丝合缝。
 */
fun biliImageThumbUrl(url: String, width: Int): String {
    if (url.isBlank()) return ""
    val normalized = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
        else -> url
    }
    if (width <= 0 || normalized.contains("@") || normalized.isGifImage()) return normalized
    val query = normalized.indexOf('?').takeIf { it >= 0 }
    val base = if (query != null) normalized.substring(0, query) else normalized
    val suffix = if (query != null) normalized.substring(query) else ""
    return "$base@${width}w.webp$suffix"
}
