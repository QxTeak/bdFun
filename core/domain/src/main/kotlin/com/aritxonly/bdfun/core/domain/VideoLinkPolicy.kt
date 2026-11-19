package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.trimUrlTrailing

sealed interface VideoTitleTarget {
    data class Bvid(val value: String) : VideoTitleTarget
    data class Aid(val value: Long) : VideoTitleTarget
    data class Short(val value: String) : VideoTitleTarget
    data object Unsupported : VideoTitleTarget
}

/**
 * B 站视频链接的识别规则。
 *
 * 短链域名除 `b23.tv` 外还有官方的 `bili2233.cn`；链接允许不带协议头
 * （分享文案里经常被剥掉 `https://`），识别与解析都走同一份规则。
 */
private const val SHORT_HOST = "(?:b23\\.tv|bili2233\\.cn)"
private const val VIDEO_HOST = "(?:(?:www\\.|m\\.)?bilibili\\.com/video/)"

private val tokenPattern = Regex(
    "(?i)" +
        "((?:https?://)?${VIDEO_HOST}(?:BV[0-9A-Za-z]{10}|av\\d+)[^\\s\\u3000<>\"']*)" +
        "|((?:https?://)?${SHORT_HOST}/[0-9A-Za-z]+)" +
        "|((?<![0-9A-Za-z])BV[0-9A-Za-z]{10})" +
        "|((?<![0-9A-Za-z])av\\d{3,})",
)

private val bvRegex = Regex("(?i)BV[0-9A-Za-z]{10}")
private val shortRegex = Regex("(?i)(?:https?://)?${SHORT_HOST}/[0-9A-Za-z]+")
private val videoRegex = Regex("(?i)(?:https?://)?${VIDEO_HOST}[0-9A-Za-z]+")
private val aidUrlRegex = Regex("(?i)(?:https?://)?${VIDEO_HOST}av(\\d+)")

fun videoLinkTokens(text: String) = tokenPattern.findAll(text).map { trimUrlTrailing(it.value) }.distinct().toList()
fun isVideoLinkToken(token: String) = tokenPattern.matches(token)
fun recognizesVideoLink(text: String) =
    bvRegex.containsMatchIn(text) || shortRegex.containsMatchIn(text) || videoRegex.containsMatchIn(text)
fun directVideoBvid(text: String): String? = bvRegex.find(text)?.value?.let { "BV" + it.substring(2) }
fun directVideoAid(text: String): Long? = aidUrlRegex.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
fun shortVideoUrl(text: String): String? = shortRegex.find(text)?.value

/** 短链统一补协议头；其余链接由调用方原样使用。 */
fun normalizedShortVideoUrl(text: String): String? =
    shortVideoUrl(text)?.let { if (it.startsWith("http", ignoreCase = true)) it else "https://$it" }

fun videoTitleTarget(token: String): VideoTitleTarget {
    val lower = token.lowercase()
    return when {
        lower.contains("b23.tv") || lower.contains("bili2233.cn") -> VideoTitleTarget.Short(token)
        lower.contains("bilibili.com/video/") ->
            bvRegex.find(token)?.value?.let { VideoTitleTarget.Bvid(it) }
                ?: directVideoAid(token)?.let { VideoTitleTarget.Aid(it) }
                ?: VideoTitleTarget.Unsupported
        token.startsWith("BV", ignoreCase = true) -> VideoTitleTarget.Bvid("BV" + token.substring(2))
        token.startsWith("av", ignoreCase = true) -> token.substring(2).toLongOrNull()?.let { VideoTitleTarget.Aid(it) } ?: VideoTitleTarget.Unsupported
        else -> VideoTitleTarget.Unsupported
    }
}
