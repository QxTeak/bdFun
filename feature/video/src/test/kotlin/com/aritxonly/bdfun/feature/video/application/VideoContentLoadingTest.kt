package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.model.video.VideoDetailOwner

import com.aritxonly.bdfun.core.model.video.VideoDetailInfo

import com.aritxonly.bdfun.core.model.video.VideoDetailCounts

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class VideoContentLoadingTest {
    private val detail = VideoDetail(
        bvid = "one",
        aid = 42,
        title = "标题",
        cover = "",
        desc = "简介",
        owner = VideoDetailOwner(
            ownerName = "UP",
            ownerFace = "",
            ownerMid = 7,
            ownerSign = "",
        ),
        counts = VideoDetailCounts(
            play = 0,
            danmaku = 0,
            like = 0,
            coin = 0,
            favorite = 0,
            share = 0,
            reply = 0,
        ),
        info = VideoDetailInfo(
            pubdate = 0,
            duration = 10,
            cid = 12,
            tname = "分区",
            pages = emptyList(),
        ),
    )
    private fun play(id: Int) = PlayInfo(emptyList(), null, "source-$id", 10000, listOf(id))
    private val item = DanmakuItem(0, 1, 0, 1f, "弹幕")
    private class Videos(var detail: VideoDetail) : VideoRepository {
        var gate: CompletableDeferred<Unit>? = null
        var error: AppError? = null
        override suspend fun videoDetail(bvid: String): Outcome<VideoDetail> {
            gate?.await()
            return error?.let { Outcome.Failure(it) } ?: Outcome.Success(detail.copy(bvid = bvid))
        }
        override suspend fun related(bvid: String): Outcome<List<VideoItem>> = Outcome.Success(emptyList())
    }
    private inner class Playback : VideoPlaybackRepository {
        var commentsGate: CompletableDeferred<Unit>? = null
        var playGate: CompletableDeferred<Unit>? = null
        var source: suspend (VideoStreamRequest) -> Outcome<PlayInfo> = { Outcome.Success(play(it.quality)) }
        var comments: suspend (Long) -> List<DanmakuItem> = { listOf(item) }
        val requests = mutableListOf<VideoStreamRequest>()
        val danmakuRequests = mutableListOf<Pair<Long, Int>>()
        val resumeRequests = mutableListOf<Pair<Long, Int>>()
        override suspend fun playInfo(request: VideoStreamRequest): Outcome<PlayInfo> {
            requests += request
            playGate?.await()
            return source(request)
        }
        override suspend fun danmaku(cid: Long, durationSec: Int): Outcome<List<DanmakuItem>> {
            danmakuRequests += cid to durationSec
            commentsGate?.await()
            return Outcome.Success(comments(cid))
        }
        override suspend fun resumePosition(detail: VideoDetail): Outcome<Long> {
            resumeRequests += detail.cid to detail.duration
            return Outcome.Success(321L)
        }
    }
    private suspend fun VideoDetailStateHolder.load(resume: Boolean = true, ready: (Long) -> Unit = {}) =
        load("one", 80, "hevc", "base", { resume }, {}, ready)

    @Test fun historyCidSelectsTheSamePartForTheStreamDanmakuAndCloudProgress() = runBlocking {
        val video = detail.copy(info = detail.info.copy(pages = listOf(
            VideoPage(12L, 1, "第一段", 10),
            VideoPage(13L, 2, "第二段", 55),
        )))
        val playback = Playback()
        val holder = VideoDetailStateHolder(Videos(video), playback = playback)
        var activeCid = 0L
        assertTrue(holder.load("one", 80, "hevc", "base", { true }, { activeCid = it }, {}, requestedCid = 13L))
        val stream = playback.requests.single()
        assertEquals(13L, activeCid)
        assertEquals(13L, stream.cid)
        assertEquals(55, stream.durationSec)
        assertEquals(listOf(13L to 55), playback.danmakuRequests)
        assertEquals(listOf(13L to 55), playback.resumeRequests)
        assertEquals(12L, holder.state.value.detail?.cid)
    }

    @Test fun staleHistoryCidUsesDefaultPartAndDoesNotReuseItsProgress() = runBlocking {
        val playback = Playback()
        val holder = VideoDetailStateHolder(Videos(detail), playback = playback)
        val selection = VideoPlaybackSelectionStateHolder("one", 99L, 80, 33_000L)
        assertTrue(holder.load(
            "one", 80, "hevc", "base",
            { selection.state.value.resumePositionMs <= 0L },
            { selection.restoreCid(it, requestedCid = 99L) },
            selection::offerCloudResume,
            requestedCid = 99L,
        ))
        assertEquals(12L, selection.state.value.cid)
        assertEquals(321L, selection.state.value.resumePositionMs)
        assertEquals(listOf(12L to 10), playback.resumeRequests)
        assertEquals(12L, playback.requests.single().cid)
    }

    @Test fun playbackBecomesReadyBeforeSlowDanmaku() = runBlocking {
        val playback = Playback().apply { commentsGate = CompletableDeferred() }
        val holder = VideoDetailStateHolder(Videos(detail), playback = playback)
        val ready = CompletableDeferred<Long>()
        val job = launch { holder.load(ready = { ready.complete(it) }) }
        assertEquals(321L, ready.await())
        assertFalse(holder.state.value.loading)
        assertNotNull(holder.state.value.playInfo)
        assertTrue(holder.state.value.danmaku.isEmpty())
        assertFalse(job.isCompleted)
        playback.commentsGate!!.complete(Unit); job.join()
        assertEquals(listOf(item), holder.state.value.danmaku)
        assertEquals("hevc", playback.requests.single().codecPreference)
    }

    @Test fun cancelledDetailReleasesLoadingAndCanBeRetried() = runBlocking {
        val videos = Videos(detail).apply { gate = CompletableDeferred() }
        val holder = VideoDetailStateHolder(videos, playback = Playback())
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        job.cancelAndJoin()
        assertFalse(holder.state.value.loading)
        assertNull(holder.state.value.error)
        videos.gate = null
        assertTrue(holder.load())
        assertNotNull(holder.state.value.playInfo)
    }

    @Test fun detailFailureDoesNotRequestStreamsOrLeaveLoading() = runBlocking {
        val videos = Videos(detail).apply { error = AppError.Network("offline") }
        val playback = Playback()
        val holder = VideoDetailStateHolder(videos, playback = playback)
        assertFalse(holder.load())
        assertEquals(videos.error, holder.state.value.error)
        assertTrue(playback.requests.isEmpty())
        assertFalse(holder.state.value.loading)
    }

    @Test fun olderQualityCannotOverwriteLatestSelection() = runBlocking {
        val old = CompletableDeferred<Outcome<PlayInfo>>()
        val playback = Playback().apply { source = { if (it.quality == 80) old.await() else Outcome.Success(play(64)) } }
        val holder = VideoDetailStateHolder(Videos(detail), playback = playback)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.switchStream(VideoStreamRequest(detail, 80), false) }
        holder.switchStream(VideoStreamRequest(detail, 64), false)
        old.complete(Outcome.Success(play(80))); job.join()
        assertEquals(play(64), holder.state.value.playInfo)
    }

    @Test fun changingQualityWhileInitialDanmakuLoadsKeepsDanmaku() = runBlocking {
        val playback = Playback().apply { commentsGate = CompletableDeferred() }
        val holder = VideoDetailStateHolder(Videos(detail), playback = playback)
        val ready = CompletableDeferred<Unit>()
        val job = launch { holder.load(ready = { ready.complete(Unit) }) }
        ready.await()
        holder.switchStream(VideoStreamRequest(detail, 64), false)
        playback.commentsGate!!.complete(Unit); job.join()
        assertEquals(play(64), holder.state.value.playInfo)
        assertEquals(listOf(item), holder.state.value.danmaku)
    }

    @Test fun changingPageDiscardsInitialPageDanmaku() = runBlocking {
        val initialComments = CompletableDeferred<List<DanmakuItem>>()
        val playback = Playback().apply { comments = { if (it == detail.cid) initialComments.await() else emptyList() } }
        val holder = VideoDetailStateHolder(Videos(detail), playback = playback)
        val ready = CompletableDeferred<Unit>()
        val job = launch { holder.load(ready = { ready.complete(Unit) }) }
        ready.await()
        holder.switchStream(VideoStreamRequest(detail, 64, cid = 13), true)
        initialComments.complete(listOf(item)); job.join()
        assertTrue(holder.state.value.danmaku.isEmpty())
        assertEquals(play(64), holder.state.value.playInfo)
    }

    @Test fun offlineRestoreInvalidatesInFlightDetail() = runBlocking {
        val videos = Videos(detail).apply { gate = CompletableDeferred() }
        val holder = VideoDetailStateHolder(videos, playback = Playback())
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        holder.restore(detail.copy(bvid = "offline"), play(32), emptyList(), emptyList())
        videos.gate!!.complete(Unit); job.join()
        assertEquals("offline", holder.state.value.detail?.bvid)
        assertEquals(play(32), holder.state.value.playInfo)
    }
}
