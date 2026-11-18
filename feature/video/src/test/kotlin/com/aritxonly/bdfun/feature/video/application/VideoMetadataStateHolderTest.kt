package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class VideoMetadataStateHolderTest {
    private val track = SubtitleTrack("one", "中文", "url")
    private val cue = SubtitleCue(0, 1000, "字幕")
    private inner class Fake : VideoMetadataRepository {
        var cuesGate: CompletableDeferred<Unit>? = null
        var online: suspend (Long) -> Outcome<Pair<Long, String>> = { Outcome.Success(it to "$it") }
        var send: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
        override suspend fun relation(mid: Long): Outcome<Int?> = Outcome.Success(null)
        override suspend fun followers(mid: Long): Outcome<Long> = Outcome.Success(100)
        override suspend fun videoCount(mid: Long): Outcome<Long> = Outcome.Success(20)
        override suspend fun online(aid: Long, cid: Long) = online(cid)
        override suspend fun subtitles(aid: Long, cid: Long): Outcome<List<SubtitleTrack>> = Outcome.Success(listOf(track))
        override suspend fun subtitleCues(url: String): Outcome<List<SubtitleCue>> {
            cuesGate?.await()
            return Outcome.Success(listOf(cue))
        }
        override suspend fun likeDanmaku(cid: Long, id: Long) = send()
        override suspend fun sendDanmaku(post: DanmakuPost) = send()
    }

    @Test fun likesDeduplicatePerPartAndRejectInvalidIds() = runBlocking {
        var calls = 0
        val fake = Fake().apply { send = { calls++; Outcome.Success(Unit) } }
        val holder = VideoMetadataStateHolder(fake)
        assertFalse(holder.likeDanmaku(0, 1))
        assertTrue(holder.likeDanmaku(1, 10))
        assertTrue(holder.likeDanmaku(1, 10))
        assertTrue(holder.likeDanmaku(2, 10))
        assertEquals(2, calls)
    }

    @Test fun cancelledLikeCanRetry() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { send = { gate.await(); Outcome.Success(Unit) } }
        val holder = VideoMetadataStateHolder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.likeDanmaku(1, 10) }
        job.cancelAndJoin()
        gate.complete(Unit)
        assertTrue(holder.likeDanmaku(1, 10))
    }

    @Test fun oldCuesCannotReappearAfterSubtitlesAreDisabled() = runBlocking {
        val fake = Fake().apply { cuesGate = CompletableDeferred() }
        val holder = VideoMetadataStateHolder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.selectSubtitle(track) }
        holder.selectSubtitle(null)
        fake.cuesGate!!.complete(Unit); job.join()
        assertNull(holder.state.value.selectedSubtitle)
        assertTrue(holder.state.value.subtitleCues.isEmpty())
    }
    @Test fun changingPageInvalidatesPendingCues() = runBlocking {
        val fake = Fake().apply { cuesGate = CompletableDeferred() }
        val holder = VideoMetadataStateHolder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.selectSubtitle(track) }
        holder.loadSubtitles(1, 2)
        fake.cuesGate!!.complete(Unit); job.join()
        assertEquals(listOf(track), holder.state.value.subtitles)
        assertNull(holder.state.value.selectedSubtitle)
        assertTrue(holder.state.value.subtitleCues.isEmpty())
    }
    @Test fun oldOnlineResponseCannotOverwriteCurrentPage() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { online = { if (it == 1L) gate.await(); Outcome.Success(it to "$it") } }
        val holder = VideoMetadataStateHolder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadOnline(1, 1) }
        holder.loadOnline(1, 2)
        gate.complete(Unit); job.join()
        assertEquals(2L, holder.state.value.onlineCount)
    }
    @Test fun absentRelationRetainsDetailFallback() = runBlocking {
        val holder = VideoMetadataStateHolder(Fake())
        var followed = false
        holder.loadAuthor(1, true, true) { followed = it }
        assertTrue(followed)
        assertEquals(100L, holder.state.value.followers)
    }
    @Test fun cancelledDanmakuSendPropagatesCancellation() = runBlocking {
        val holder = VideoMetadataStateHolder(Fake().apply { send = { throw CancellationException() } })
        try {
            holder.sendDanmaku(DanmakuPost(1, "one", "text", 0, 1, 25, 0))
            fail("取消必须传播")
        } catch (_: CancellationException) { }
    }
}
