package com.aritxonly.bdfun.infra.bili

import android.net.Uri
import android.text.Html
import java.security.MessageDigest

internal const val BILI_API = "https://api.bilibili.com"
internal const val BILI_APP_API = "https://app.bilibili.com"
internal const val BILI_APP_UA =
    "Mozilla/5.0 BiliDroid/8.43.0 (bbcallen@gmail.com) os/android model/android " +
        "mobi_app/android build/8430300 channel/master innerVer/8430300 osVer/15 network/2"
internal const val BILI_APP = "https://app.bilibili.com"

/**
 * App 推荐用的 HD 端 UA / trace-id / statistics。
 *
 * 与 PiliPlus 的 `rcmdVideoListApp` 完全一致：`app-key: android_hd` + `mobi_app: android_i`
 * + 这份 2.0.1 HD UA 是 B 站客户端真实组合，缺任意一项推荐流都会被降级。
 */
internal const val BILI_APP_HD_UA =
    "Mozilla/5.0 BiliDroid/2.0.1 (bbcallen@gmail.com) os/android model/android_hd " +
        "mobi_app/android_hd build/2001100 channel/master innerVer/2001100 osVer/15 network/2"
internal const val BILI_APP_TRACE_ID =
    "11111111111111111111111111111111:1111111111111111:0:0"
internal const val BILI_APP_STATISTICS =
    """{"appId":1,"platform":3,"version":"8.43.0","abtest":""}"""
/** Android App 的 appkey / appsec，用于 app.bilibili.com 的签名。 */
internal const val BILI_APP_KEY = "dfca71928277209b"
internal const val BILI_APP_SEC = "b5475a8825547a4fc26c7d518eaaa02e"
/** TV 端 appkey，用于 /x/passport-tv-login 扫码换 access_key。 */
internal const val BILI_TV_APP_KEY = "4409e2ce8ffd12b8"
internal const val BILI_TV_APP_SEC = "59b43e04ad6965f34319062b478f83dd"
internal val BILI_APP_HEADERS = mapOf(
    "env" to "prod",
    "app-key" to "android64",
    "x-bili-aurora-zone" to "sh001",
)
internal const val BILI_PASSPORT = "https://passport.bilibili.com"
internal const val BILI_VC = "https://api.vc.bilibili.com"
internal const val BILI_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

internal fun encodeBiliParameter(value: String): String = Uri.encode(value, "-_.~")

internal fun biliMd5(value: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

internal fun biliImage(url: String, width: Int = 0): String =
    com.aritxonly.bdfun.core.common.biliImageUrl(url, width)

internal fun biliAvatar(url: String, size: Int): String {
    if (url.isBlank()) return ""
    var result = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
        else -> url
    }
    if (!result.contains("@")) {
        result += "@${size}w_${size}h_1c.webp"
    }
    return result
}

internal fun cleanBiliHtml(text: String): String {
    if (text.isBlank()) return ""
    return runCatching {
        Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString()
    }.getOrDefault(text.replace(Regex("<[^>]+>"), ""))
}

/** App 推荐接口只给 aid，不给 bvid；用 AV/BV 官方换算规则在本地转。 */
internal fun biliAidToBvid(aid: Long): String {
    if (aid <= 0L) return ""
    val table = "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF"
    val positions = intArrayOf(11, 10, 3, 8, 4, 6)
    val result = charArrayOf('B', 'V', '1', ' ', ' ', '4', ' ', '1', ' ', '7', ' ', ' ')
    val x = (aid xor 177451812L) + 8728348608L
    var power = 1L
    for (i in 0..5) {
        result[positions[i]] = table[((x / power) % 58L).toInt()]
        power *= 58L
    }
    return String(result)
}

/** 动态接口的播放量/弹幕数有时是「14.6万」「1.2亿」这种文字，不能直接 toLong。 */
internal fun parseBiliCountText(text: String): Long {
    val raw = text.trim()
    if (raw.isBlank() || raw == "-" || raw == "--") return 0L
    val multiplier = when {
        raw.endsWith("亿") -> 100_000_000.0
        raw.endsWith("万") -> 10_000.0
        else -> 1.0
    }
    val number = raw.removeSuffix("亿").removeSuffix("万").toDoubleOrNull() ?: return 0L
    return (number * multiplier).toLong()
}

internal fun parseBiliDurationText(text: String): Int {
    if (text.isBlank() || text == "0") return 0
    return runCatching {
        val parts = text.split(":")
        when (parts.size) {
            3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
            2 -> parts[0].toInt() * 60 + parts[1].toInt()
            else -> text.toIntOrNull() ?: 0
        }
    }.getOrDefault(0)
}

internal fun biliQualityLabel(id: Int): String = when (id) {
    127 -> "8K"
    126 -> "杜比视界"
    125 -> "HDR"
    120 -> "4K"
    116 -> "1080P60"
    112 -> "1080P+"
    80 -> "1080P"
    74 -> "720P60"
    64 -> "720P"
    32 -> "480P"
    16 -> "360P"
    else -> "${id}P"
}
