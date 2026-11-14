package com.aritxonly.bdfun.feature.home.application

import com.aritxonly.bdfun.core.application.RecommendationOptimizer
import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class HomeOptimizationIntegrationTest {
    private fun video(id: Int, owner: Long = id.toLong()) = VideoItem("v$id", id.toLong(), "", "", "", "", owner, 0, 0, 60, 0)
    private class Feed : FeedRepository {
        val calls = mutableListOf<Int>()
        var load: suspend (Int) -> List<VideoItem> = { listOf(VideoItem("v1", 1, "", "", "", "", 1, 0, 0, 60, 0)) }
        override suspend fun recommend(page: Int): Outcome<List<VideoItem>> { calls += page; return Outcome.Success(load(page)) }
        override suspend fun dynamics(offset: String): Outcome<FeedPage> = error("unused")
    }
    private val videos = object : VideoRepository {
        override suspend fun videoDetail(bvid: String): Outcome<VideoDetail> = error("unused")
        override suspend fun related(bvid: String): Outcome<List<VideoItem>> = error("unused")
    }
    private fun optimizer(scope: CoroutineScope) = RecommendationOptimizer(object : RecommendationStorage {
        override fun read() = RecommendationArchive()
        override fun write(archive: RecommendationArchive) = Unit
    }, scope, { "a" to 0L }, io = Dispatchers.Unconfined)

    @Test fun disabledPathRetainsOriginalOrderAndRequestCount() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val optimization = optimizer(scope)
            val items = listOf(video(1), video(2, 1), video(3))
            val feed = Feed().apply { load = { items } }
            val state = HomeFeedStateHolder(feed, videos, optimization)
            state.ensureLoaded(); state.loadMore()
            assertEquals(items, state.state.value.videos)
            assertEquals(listOf(1, 2), feed.calls)
            assertTrue(state.state.value.endReached)
        } finally { scope.cancel() }
    }
    @Test fun optimizedDuplicatesHaveFiniteBudgetAndManualContinuation() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val optimization = optimizer(scope).apply { setEnabled(true) }
            val feed = Feed()
            val state = HomeFeedStateHolder(feed, videos, optimization)
            state.ensureLoaded(); state.loadMore()
            assertEquals(listOf(1, 2, 3, 4), feed.calls)
            assertFalse(state.state.value.endReached)
            assertTrue(state.state.value.filteredPage)
            state.loadMore()
            assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), feed.calls)
        } finally { scope.cancel() }
    }
    @Test fun modeSwitchRejectsInFlightResultAndNextRefreshRebuildsBaseline() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val optimization = optimizer(scope).apply { setEnabled(true) }
            val feed = Feed()
            val state = HomeFeedStateHolder(feed, videos, optimization)
            state.ensureLoaded()
            val gate = CompletableDeferred<Unit>()
            feed.load = { gate.await(); listOf(video(2)) }
            val request = launch(start = CoroutineStart.UNDISPATCHED) { state.loadMore() }
            optimization.setEnabled(false); state.modeChanged()
            gate.complete(Unit); request.join()
            assertEquals(listOf(video(1)), state.state.value.videos)
            assertFalse(state.state.value.loadingMore)
            feed.load = { listOf(video(3)) }
            state.refresh()
            assertEquals(listOf(video(3)), state.state.value.videos)
        } finally { scope.cancel() }
    }
    @Test fun optimizationNeverReordersPreviouslyPublishedItems() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val optimization = optimizer(scope).apply { setEnabled(true) }
            val feed = Feed().apply { load = { page -> if (page == 1) listOf(video(1), video(2)) else listOf(video(3), video(4)) } }
            val state = HomeFeedStateHolder(feed, videos, optimization)
            state.ensureLoaded()
            val old = state.state.value.videos
            optimization.open(video(2))?.let { optimization.watch(video(2), it, 1.0) }
            state.loadMore()
            assertEquals(old, state.state.value.videos.take(old.size))
        } finally { scope.cancel() }
    }
    @Test fun switchingModesRestartsPaginationAfterRebuildingTheList() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val optimization = optimizer(scope).apply { setEnabled(true) }
            val feed = Feed().apply { load = { listOf(video(it)) } }
            val state = HomeFeedStateHolder(feed, videos, optimization)
            state.ensureLoaded(); state.loadMore()
            optimization.setEnabled(false); state.modeChanged()
            state.refresh(); state.loadMore()
            assertEquals(listOf(1, 2, 1, 2), feed.calls)
        } finally { scope.cancel() }
    }

}
