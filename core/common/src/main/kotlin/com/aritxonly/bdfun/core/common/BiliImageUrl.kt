package com.aritxonly.bdfun.core.common

/** 沿用旧图片 URL 规则；已带裁切参数的链接不能再追加尺寸。 */
fun biliImageUrl(url: String, width: Int = 0): String {
    if (url.isBlank()) return ""
    var result = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
        else -> url
    }
    if (width > 0 && !result.contains("@")) {
        result += "@${width}w_${(width * 9 / 16)}h_1c.webp"
    }
    return result
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
    if (width <= 0 || normalized.contains("@")) return normalized
    return "$normalized@${width}w.webp"
}
