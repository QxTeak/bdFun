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

class VideoDetailStateHolderTest {
    private val video = VideoItem("one", 42, "one", "", "UP", "", 7, 0, 0, 0, 0)
    private val playback = object : VideoPlaybackRepository {
        override suspend fun playInfo(request: VideoStreamRequest): Outcome<PlayInfo> = Outcome.Failure(AppError.Network("offline"))
        override suspend fun danmaku(cid: Long, durationSec: Int): Outcome<List<DanmakuItem>> = Outcome.Success(emptyList())
        override suspend fun resumePosition(detail: VideoDetail): Outcome<Long> = Outcome.Success(0L)
    }
    private fun holder(repository: VideoRepository, related: List<VideoItem> = emptyList()) =
        VideoDetailStateHolder(repository, related, playback)
    private class Fake : VideoRepository {
        var relatedResult: Outcome<List<VideoItem>> = Outcome.Success(emptyList())
        var detailResult: Outcome<VideoDetail> = Outcome.Failure(AppError.Network("offline"))
        var pgcResult: Outcome<VideoDetail> = Outcome.Failure(AppError.Network("offline"))
        var cancel = false
        val requests = mutableListOf<String>()
        val pgcRequests = mutableListOf<Pair<Long, Long>>()
        override suspend fun videoDetail(bvid: String): Outcome<VideoDetail> {
            requests += bvid
            if (cancel) throw CancellationException()
            return detailResult
        }
        override suspend fun related(bvid: String): Outcome<List<VideoItem>> {
            requests += bvid
            if (cancel) throw CancellationException()
            return relatedResult
        }
        override suspend fun pgcEpisode(epId: Long, seasonId: Long): Outcome<VideoDetail> {
            pgcRequests += epId to seasonId
            if (cancel) throw CancellationException()
            return pgcResult
        }
    }

    @Test fun cachedRecommendationsAreAvailableBeforeNetwork() {
        val items = mutableListOf(video)
        val holder = holder(Fake(), items)
        items.clear()
        assertEquals(listOf(video), holder.state.value.related)
    }

    @Test fun failedRefreshDoesNotEraseCachedRecommendations() = runBlocking {
        val repository = Fake().apply { relatedResult = Outcome.Failure(AppError.Network("offline")) }
        val holder = holder(repository, listOf(video))
        assertTrue(holder.fetchRelated("one").isEmpty())
        assertEquals(listOf(video), holder.state.value.related)
    }

    @Test fun offlineRestoreCanClearRecommendationsWithoutNetwork() {
        val repository = Fake()
        val holder = holder(repository, listOf(video))
        holder.restoreRelated(emptyList())
        assertTrue(holder.state.value.related.isEmpty())
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun detailErrorAndRequestedEpisodeArePreserved() = runBlocking {
        val repository = Fake()
        assertEquals(repository.detailResult, holder(repository).fetchDetail("episode"))
        assertEquals(listOf("episode"), repository.requests)
    }

    @Test fun restoringDetailKeepsRecommendationsAndSupportsEpisodeReplacement() {
        val holder = holder(Fake(), listOf(video))
        val detail = VideoDetail(
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
        holder.restoreDetail(detail)
        assertEquals(detail, holder.state.value.detail)
        holder.restoreDetail(detail.copy(
            bvid = "episode",
            info = detail.info.copy(
                cid = 13,
            ),
        ))
        assertEquals("episode", holder.state.value.detail?.bvid)
        assertEquals(listOf(video), holder.state.value.related)
    }

    @Test fun cancellationPropagatesForBothReads() = runBlocking {
        val holder = holder(Fake().apply { cancel = true })
        try { holder.fetchDetail("one"); fail("取消必须传播") } catch (_: CancellationException) { }
        try { holder.fetchRelated("one"); fail("取消必须传播") } catch (_: CancellationException) { }
    }

    /** 番剧卡片只带 season_id 进页面：必须走 PGC 读取，而不是拿空 bvid 查普通稿件。 */
    @Test fun seasonOnlyEntryLoadsPgcDetailAndPublishesEpisodeCid() = runBlocking {
        val detail = VideoDetail(
            bvid = "BV1KN4y1w784",
            aid = 901628511L,
            title = "不止考古",
            cover = "",
            desc = "",
            owner = VideoDetailOwner("UP", "", 7, ""),
            counts = VideoDetailCounts(0, 0, 0, 0, 0, 0, 0),
            info = VideoDetailInfo(1_666_260_000L, 2747, 1_146_763_843L, "纪录片",
                pages = emptyList(), epId = 689147L, seasonId = 43161L),
        )
        val repository = Fake().apply { pgcResult = Outcome.Success(detail) }
        val selected = mutableListOf<Long>()
        val holder = holder(repository)
        val ok = holder.load("", 80, "auto", "base", resume = { false },
            onPartSelected = { selected += it }, onReady = {}, seasonId = 43161L)
        assertTrue(ok)
        assertEquals(listOf(0L to 43161L), repository.pgcRequests)
        assertEquals(listOf(1_146_763_843L), selected)
        assertEquals(detail, holder.state.value.detail)
        // 详情本身有 bvid，仍会补一次相关推荐；不应再按空 bvid 查详情。
        assertEquals(listOf(detail.bvid), repository.requests)
    }
}
