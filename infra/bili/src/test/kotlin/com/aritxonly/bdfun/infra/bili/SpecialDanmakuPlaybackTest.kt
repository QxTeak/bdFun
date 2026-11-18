package com.aritxonly.bdfun.infra.bili

import com.android.purebilibili.danmaku.parser.DanmakuProto
import com.android.purebilibili.danmaku.parser.SpecialDanmakuEntry
import com.android.purebilibili.danmaku.parser.SpecialDanmakuSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SpecialDanmakuPlaybackTest {
    @Test fun seekRetainsLongScenesAndOnlyFetchesPayloadsThatCouldOverlap() = runBlocking {
        val long = record(0, "long", "1m")
        val expired = record(2_000, "short", "1s")
        val future = record(40_000, "future", "2s")
        val playback = IndexedSpecialPlayback(listOf(long.first, expired.first, future.first), "") { payload ->
            val text = payload.decodeToString()
            DanmakuProto.DanmakuElem(mode = 9, content = text)
        }
        assertEquals(listOf("long"), playback.readWindow(20_000).map { it.text.substringAfter("content=\"").substringBefore('"') })
        assertEquals(1, long.second.reads)
        assertEquals(1, expired.second.reads)
        assertEquals(0, future.second.reads)
        assertEquals(2, playback.readWindow(39_000).size)
        assertEquals(1, long.second.reads)
        assertEquals(1, expired.second.reads)
        assertEquals(1, future.second.reads)
        assertEquals(2, playback.readWindow(1_000).size)
        assertEquals(2, expired.second.reads)
    }

    private fun record(start: Int, title: String, duration: String): Pair<SpecialRecord, Source> {
        val payload = "def text node {content=\"$title\" duration=$duration}".toByteArray()
        val source = Source(payload)
        return SpecialRecord(source, SpecialDanmakuEntry(0, payload.size, start.toLong(), 9)) to source
    }

    private class Source(private val envelope: ByteArray) : SpecialDanmakuSource {
        override val byteLength = envelope.size.toLong()
        var reads = 0
        override suspend fun readRange(offset: Long, byteCount: Int): ByteArray {
            reads++
            return envelope.copyOfRange(offset.toInt(), offset.toInt() + byteCount)
        }
    }
}
