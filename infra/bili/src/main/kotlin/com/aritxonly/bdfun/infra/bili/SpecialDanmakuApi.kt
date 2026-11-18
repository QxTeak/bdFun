package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.danmakuUserHash

import com.android.purebilibili.danmaku.parser.DanmakuParser
import com.android.purebilibili.danmaku.parser.DanmakuProto
import com.android.purebilibili.danmaku.parser.SpecialDanmakuEntry
import com.android.purebilibili.danmaku.parser.SpecialDanmakuIndexReader
import com.android.purebilibili.danmaku.parser.SpecialDanmakuSource
import com.android.purebilibili.danmaku.parser.bas.BasScriptParser
import com.aritxonly.bdfun.core.domain.SpecialDanmakuPlayback
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.IOException

private const val MaxSpecialFileBytes = 2 * 1024 * 1024
private const val MaxIndexedBytes = 48L * 1024 * 1024
private const val MaxSpecialEntries = 20_000
private val ContentRange = Regex("""bytes (\d+)-(\d+)/(\d+)""")

internal suspend fun BiliApiContext.specialDanmaku(aid: Long, cid: Long): SpecialDanmakuPlayback = withContext(Dispatchers.IO) {
    if (aid <= 0 || cid <= 0) return@withContext SpecialDanmakuPlayback { emptyList() }
    val ownHash = danmakuUserHash(mid)
    val transport = BiliHttpClient(client.newBuilder().followRedirects(false).followSslRedirects(false).build(), false) { _, _ -> }
    var fallbackBudget = 4 * MaxSpecialFileBytes
    val files = danmakuView(aid, cid).specialDms.distinct().take(24)
    val index = ArrayList<SpecialRecord>()
    for (file in files) {
        val url = (if (file.startsWith("//")) "https:$file" else file).toHttpUrlOrNull() ?: continue
        if (!url.isBiliDanmakuCdn()) continue
        val source = cancellableCatching { transport.openSpecialSource(url, fallbackBudget) }.getOrNull() ?: continue
        if (source is InMemorySpecialSource) fallbackBudget -= source.byteLength.toInt()
        val reader = SpecialDanmakuIndexReader(source.byteLength, source::readRange)
        while (index.size < MaxSpecialEntries) {
            currentCoroutineContext().ensureActive()
            val item = cancellableCatching { reader.next() }.getOrNull() ?: break
            if ((item.mode == 7 || item.mode == 9) && item.byteLength <= 128 * 1024) index += SpecialRecord(source, item)
        }
        if (index.size >= MaxSpecialEntries) break
    }
    IndexedSpecialPlayback(index.sortedBy { it.entry.startTimeMs }, ownHash)
}

private fun HttpUrl.isBiliDanmakuCdn(): Boolean = isHttps &&
    listOf("hdslb.com", "bilibili.com", "bilivideo.com", "bilivideo.cn").any { host == it || host.endsWith(".$it") }

private fun specialRequest(url: HttpUrl, range: String): Request = Request.Builder().url(url)
    .header("Referer", "https://www.bilibili.com/").header("Accept-Encoding", "identity").header("Range", range).build()

private suspend fun BiliHttpClient.openSpecialSource(url: HttpUrl, fallbackBudget: Int): SpecialDanmakuSource? =
    await(specialRequest(url, "bytes=0-0")).use { response ->
        if (!response.request.url.isBiliDanmakuCdn()) return@use null
        val body = response.body ?: return@use null
        when (response.code) {
            206 -> {
                val range = response.header("Content-Range")?.let(ContentRange::matchEntire) ?: return@use null
                if (range.groupValues[1] != "0" || range.groupValues[2] != "0") return@use null
                val length = range.groupValues[3].toLongOrNull()?.takeIf { it in 1..MaxIndexedBytes } ?: return@use null
                object : SpecialDanmakuSource {
                    override val byteLength = length
                    override suspend fun readRange(offset: Long, byteCount: Int) = readSpecialRange(url, offset, byteCount, length)
                }
            }
            200 -> {
                val limit = minOf(MaxSpecialFileBytes, fallbackBudget)
                if (limit <= 0 || body.contentLength() > limit) return@use null
                val bytes = body.byteStream().readNBytes(limit + 1)
                if (bytes.size > limit) return@use null
                InMemorySpecialSource(bytes)
            }
            else -> null
        }
    }

private suspend fun BiliHttpClient.readSpecialRange(url: HttpUrl, offset: Long, count: Int, length: Long): ByteArray {
    val end = offset + count - 1L
    val expected = "bytes $offset-$end/$length"
    return await(specialRequest(url, "bytes=$offset-$end")).use { response ->
        if (response.code != 206 || response.header("Content-Range") != expected || !response.request.url.isBiliDanmakuCdn()) {
            throw IOException("特殊弹幕范围不匹配")
        }
        response.body?.byteStream()?.readNBytes(count + 1)?.takeIf { it.size == count }
            ?: throw IOException("特殊弹幕范围长度不匹配")
    }
}

private class InMemorySpecialSource(private val bytes: ByteArray) : SpecialDanmakuSource {
    override val byteLength = bytes.size.toLong()
    override suspend fun readRange(offset: Long, byteCount: Int): ByteArray =
        bytes.copyOfRange(offset.toInt(), offset.toInt() + byteCount)
}

internal class SpecialRecord(val source: SpecialDanmakuSource, val entry: SpecialDanmakuEntry) {
    var duration: Long? = null
    var item: DanmakuItem? = null
}

internal class IndexedSpecialPlayback(
    private val index: List<SpecialRecord>, private val ownHash: String,
    private val decode: (ByteArray) -> DanmakuProto.DanmakuElem? = { DanmakuProto.parseReply(it).elems.singleOrNull() },
) : SpecialDanmakuPlayback {
    private var previous: List<DanmakuItem> = emptyList()

    override suspend fun readWindow(positionMs: Long): List<DanmakuItem> = withContext(Dispatchers.IO) {
        val start = positionMs.coerceAtLeast(0)
        val end = start.coerceAtMost(Long.MAX_VALUE - 6_000L) + 6_000L
        val result = ArrayList<DanmakuItem>()
        for (record in index) {
            currentCoroutineContext().ensureActive()
            if (record.entry.startTimeMs >= end || record.hasEnded(start)) {
                record.item = null
                continue
            }
            if (record.item == null) cancellableCatching { loadRecord(record, start) }
            record.item?.let(result::add)
        }
        if (previous != result) previous = result
        previous
    }

    private fun SpecialRecord.hasEnded(position: Long): Boolean =
        duration?.let { it <= 0 || position - entry.startTimeMs >= it } == true

    private suspend fun loadRecord(record: SpecialRecord, start: Long) {
        val envelope = record.source.readRange(record.entry.offset, record.entry.byteLength)
        val elem = decode(envelope)
        if (elem == null || elem.content.length > 65_536) {
            record.duration = 0
            return
        }
        if (record.duration == null) record.duration = when (elem.mode) {
            9 -> runCatching { BasScriptParser.parseDurationMs(elem.content) }.getOrDefault(0)
            7 -> DanmakuParser.parseElement(elem).advancedList.singleOrNull()?.durationMs ?: 0
            else -> 0
        }
        if (!record.hasEnded(start)) record.item = DanmakuItem(
            elem.progress.toLong(), elem.mode, elem.color, elem.fontsize.toFloat(), elem.content, elem.id,
            userHash = elem.midHash, self = ownHash.isNotEmpty() && ownHash.equals(elem.midHash, ignoreCase = true),
        )
    }
}
