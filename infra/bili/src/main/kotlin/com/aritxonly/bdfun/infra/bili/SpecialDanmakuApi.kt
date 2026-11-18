package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.video.DanmakuItem
import com.aritxonly.bdfun.infra.bili.mapper.mapDanmakuSegments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

private const val MaxSpecialFileBytes = 2 * 1024 * 1024

/** 特殊弹幕只接受元数据提供的 B 站 CDN 地址，单次解析时保留一份有界文件。 */
internal suspend fun BiliApiContext.specialDanmaku(aid: Long, cid: Long): List<DanmakuItem> = withContext(Dispatchers.IO) {
    if (aid <= 0 || cid <= 0) return@withContext emptyList()
    val ownHash = danmakuUserHash(mid)
    val files = danmakuView(aid, cid).specialDms.distinct().take(24)
    buildList {
        for (file in files) {
            val url = (if (file.startsWith("//")) "https:$file" else file).toHttpUrlOrNull() ?: continue
            if (!url.isHttps || listOf("hdslb.com", "bilibili.com", "bilivideo.com", "bilivideo.cn")
                    .none { url.host == it || url.host.endsWith(".$it") }) continue
            val request = Request.Builder().url(url).header("Referer", "https://www.bilibili.com/").build()
            val bytes = cancellableCatching {
                http.await(request).use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body ?: return@use null
                    if (body.contentLength() > MaxSpecialFileBytes) return@use null
                    body.byteStream().readNBytes(MaxSpecialFileBytes + 1).takeIf { it.size <= MaxSpecialFileBytes }
                }
            }.getOrNull() ?: continue
            addAll(mapDanmakuSegments(listOf(bytes), ownHash).filter { it.mode == 7 || it.mode == 9 })
        }
    }.distinctBy { Triple(it.id, it.timeMs, it.text) }
}
