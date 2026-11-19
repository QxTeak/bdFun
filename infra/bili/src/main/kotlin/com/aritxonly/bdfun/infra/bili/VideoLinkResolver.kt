package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.directVideoAid
import com.aritxonly.bdfun.core.domain.directVideoBvid
import com.aritxonly.bdfun.core.domain.normalizedShortVideoUrl
import java.net.HttpURLConnection
import java.net.URL

/**
 * 外部链接/剪贴板解析：BV、av 与短链（b23.tv / bili2233.cn）都走这里。
 * 短链最多跟 4 跳，超时 5 秒，失败静默返回 null（沿用原外部入口的边界）。
 */
fun resolveVideoLink(text: String, open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }): String? {
    directVideoBvid(text)?.let { return it }
    directVideoAid(text)?.let { return biliAidToBvid(it) }
    val short = normalizedShortVideoUrl(text) ?: return null
    return runCatching {
        var url = URL(short)
        repeat(4) {
            val conn = open(url).apply {
                instanceFollowRedirects = false
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            conn.connect()
            val location = conn.getHeaderField("Location")
            conn.disconnect()
            if (location.isNullOrBlank()) return@repeat
            url = URL(url, location)
            directVideoBvid(url.toString())?.let { return it }
            directVideoAid(url.toString())?.let { return biliAidToBvid(it) }
        }
        null
    }.getOrNull()
}
