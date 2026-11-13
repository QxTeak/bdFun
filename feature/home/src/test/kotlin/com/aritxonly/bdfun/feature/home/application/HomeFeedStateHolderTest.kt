package com.aritxonly.bdfun.feature.home.application

import com.aritxonly.bdfun.core.model.video.VideoItemExtras

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.feed.FeedPage
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class HomeFeedStateHolderTest {
    private fun item(id: String, owner: Long = 1, category: String = "游戏") =
        VideoItem(
            bvid = id,
            aid = 1,
            title = id,
            cover = "",
            ownerName = "",
            ownerFace = "",
            ownerMid = owner,
            play = 0,
            danmaku = 0,
            duration = 0,
            pubdate = 0,
            extras = VideoItemExtras(
                categoryName = category,
            ),
        )
    private class Feed : FeedRepository {
        val requests = mutableListOf<Int>()
        var load: suspend (Int) -> Outcome<List<VideoItem>> = { Outcome.Success(emptyList()) }
        override suspend fun recommend(page: Int): Outcome<List<VideoItem>> { requests += page; return load(page) }
        override suspend fun dynamics(offset: String): Outcome<FeedPage> = error("unused")
    }
    private val videoRepository = object : VideoRepository {
        override suspend fun videoDetail(bvid: String): Outcome<VideoDetail> = Outcome.Failure(AppError.Network("offline"))
        override suspend fun related(bvid: String): Outcome<List<VideoItem>> = Outcome.Success(emptyList())
    }

    @Test fun firstPageDeduplicatesAndReturningDoesNotRefetch() = runBlocking {
        val feed = Feed().apply { load = { Outcome.Success(listOf(item("a"), item("a"), item("b"))) } }
        val state = HomeFeedStateHolder(feed, videoRepository)
        state.ensureLoaded(); state.ensureLoaded()
        assertEquals(listOf(1), feed.requests)
        assertEquals(listOf("a", "b"), state.state.value.videos.map { it.bvid })
        assertFalse(state.state.value.loading)
    }
    @Test fun concurrentInitialRequestsShareOneLoad() = runBlocking {
        val pending = CompletableDeferred<Outcome<List<VideoItem>>>()
        val feed = Feed().apply { load = { pending.await() } }
        val state = HomeFeedStateHolder(feed, videoRepository)
        val first = launch(start = CoroutineStart.UNDISPATCHED) { state.ensureLoaded() }
        val second = launch(start = CoroutineStart.UNDISPATCHED) { state.ensureLoaded() }
        pending.complete(Outcome.Success(listOf(item("a"))))
        joinAll(first, second)
        assertEquals(listOf(1), feed.requests)
    }
    @Test fun refreshPrependsNewVideosRetainsOldOrderAndRequestsScroll() = runBlocking {
        val feed = Feed().apply { load = { Outcome.Success(listOf(item("a"), item("b"))) } }
        val state = HomeFeedStateHolder(feed, videoRepository)
        state.ensureLoaded()
        feed.load = { Outcome.Success(listOf(item("b"), item("c"), item("d"))) }
        state.refresh()
        assertEquals(listOf("c", "d", "a", "b"), state.state.value.videos.map { it.bvid })
        assertEquals(1L, state.state.value.scrollRevision)
        assertFalse(state.state.value.refreshing)
    }
    @Test fun paginationDeduplicatesAndRawEmptyPageEnds() = runBlocking {
        val feed = Feed().apply { load = { page -> Outcome.Success(when (page) {
            1 -> listOf(item("a"), item("b")); 2 -> listOf(item("b"), item("c")); else -> emptyList()
        }) } }
        val state = HomeFeedStateHolder(feed, videoRepository)
        state.ensureLoaded(); state.loadMore(); state.loadMore(); state.loadMore()
        assertEquals(listOf(1, 2, 3), feed.requests)
        assertEquals(listOf("a", "b", "c"), state.state.value.videos.map { it.bvid })
        assertTrue(state.state.value.endReached)
    }
    @Test fun failedPaginationCanRetryTheSamePage() = runBlocking {
        val feed = Feed().apply { load = { Outcome.Success(listOf(item("a"))) } }
        val state = HomeFeedStateHolder(feed, videoRepository)
        state.ensureLoaded()
        feed.load = { Outcome.Failure(AppError.Network("offline")) }
        state.loadMore()
        assertFalse(state.state.value.endReached)
        assertEquals(AppError.Network("offline"), state.state.value.loadMoreError)
        feed.load = { Outcome.Success(listOf(item("b"))) }
        state.loadMore()
        assertEquals(listOf(1, 2, 2), feed.requests)
        assertEquals(listOf("a", "b"), state.state.value.videos.map { it.bvid })
    }
    @Test fun excludedOnlyPageDoesNotHideLaterPages() = runBlocking {
        val feed = Feed().apply { load = { Outcome.Success(listOf(item("a"))) } }
        val state = HomeFeedStateHolder(feed, videoRepository)
        state.ensureLoaded()
        state.dismissRecommendation(item("a"), creator = true)
        feed.load = { page -> Outcome.Success(listOf(item(if (page == 2) "blocked" else "visible", if (page == 2) 1 else 2))) }
        state.loadMore()
        assertFalse(state.state.value.endReached)
        assertTrue(state.state.value.filteredPage)
        state.loadMore()
        assertEquals(listOf("visible"), state.state.value.videos.map { it.bvid })
    }
    @Test fun failedCategoryResolutionKeepsUnrelatedUnknownVideoVisible() = runBlocking {
        val feed = Feed().apply { load = { Outcome.Success(listOf(item("a"))) } }
        val state = HomeFeedStateHolder(feed, videoRepository)
        state.ensureLoaded()
        state.dismissRecommendation(item("a"), category = "游戏")
        feed.load = { Outcome.Success(listOf(item("unknown", 2, ""))) }
        state.loadMore()
        assertEquals("unknown", state.state.value.videos.single().bvid)
    }
    @Test fun refreshDiscardsLatePaginationWithoutLosingCurrentItems() = runBlocking {
        val pending = CompletableDeferred<Outcome<List<VideoItem>>>()
        val feed = Feed().apply { load = { page -> if (page == 1) Outcome.Success(listOf(item("old"))) else pending.await() } }
        val state = HomeFeedStateHolder(feed, videoRepository)
        state.ensureLoaded()
        val loading = launch(start = CoroutineStart.UNDISPATCHED) { state.loadMore() }
        feed.load = { Outcome.Success(listOf(item("new"))) }
        state.refresh()
        pending.complete(Outcome.Success(listOf(item("stale"))))
        loading.join()
        assertEquals(listOf("new", "old"), state.state.value.videos.map { it.bvid })
        assertFalse(state.state.value.loadingMore)
    }
    @Test fun cancellationClearsLoadingAndAllowsNextAttempt() = runBlocking {
        val feed = Feed().apply { load = { awaitCancellation() } }
        val state = HomeFeedStateHolder(feed, videoRepository)
        val loading = launch(start = CoroutineStart.UNDISPATCHED) { state.ensureLoaded() }
        loading.cancelAndJoin()
        assertFalse(state.state.value.loading)
        assertNull(state.state.value.error)
        feed.load = { Outcome.Success(listOf(item("retry"))) }
        state.ensureLoaded()
        assertEquals("retry", state.state.value.videos.single().bvid)
    }
    @Test fun emptySuccessfulPageHasAnExplicitEmptyState() = runBlocking {
        val feed = Feed()
        val state = HomeFeedStateHolder(feed, videoRepository)
        state.ensureLoaded()
        assertTrue(state.state.value.empty)
        assertFalse(state.state.value.loading)
        assertNull(state.state.value.error)
    }

    @Test fun closeInvalidatesPendingFirstLoadAndBlocksFutureCommands() = runBlocking {
        val pending = CompletableDeferred<Outcome<List<VideoItem>>>()
        val feed = Feed().apply { load = { pending.await() } }
        val state = HomeFeedStateHolder(feed, videoRepository)
        val request = launch(start = CoroutineStart.UNDISPATCHED) { state.ensureLoaded() }
        state.close(); state.close(); pending.complete(Outcome.Success(listOf(item("old")))); request.join()
        state.ensureLoaded(); state.refresh(); state.loadMore(); state.requestRefresh()
        assertEquals(listOf(1), feed.requests); assertTrue(state.state.value.videos.isEmpty())
        assertFalse(state.state.value.loading); assertEquals(0, state.state.value.refreshRequest)
    }
    @Test fun closeInvalidatesPendingRefreshWithoutScrollingOrPublishingError() = runBlocking {
        val feed = Feed().apply { load = { Outcome.Success(listOf(item("kept"))) } }
        val state = HomeFeedStateHolder(feed, videoRepository); state.ensureLoaded()
        val pending = CompletableDeferred<Outcome<List<VideoItem>>>()
        feed.load = { pending.await() }
        val request = launch(start = CoroutineStart.UNDISPATCHED) { state.refresh() }
        state.close(); pending.complete(Outcome.Failure(AppError.Network("late"))); request.join()
        assertEquals(listOf("kept"), state.state.value.videos.map { it.bvid })
        assertNull(state.state.value.error); assertFalse(state.state.value.refreshing)
        assertEquals(0L, state.state.value.scrollRevision)
    }
    @Test fun closeInvalidatesPaginationAndLateDismissal() = runBlocking {
        val feed = Feed().apply { load = { Outcome.Success(listOf(item("kept"))) } }
        val state = HomeFeedStateHolder(feed, videoRepository); state.ensureLoaded()
        val pending = CompletableDeferred<Outcome<List<VideoItem>>>()
        feed.load = { pending.await() }
        val request = launch(start = CoroutineStart.UNDISPATCHED) { state.loadMore() }
        state.close(); pending.complete(Outcome.Success(listOf(item("late")))); request.join()
        state.dismissRecommendation(item("kept"))
        assertEquals(listOf("kept"), state.state.value.videos.map { it.bvid })
        assertFalse(state.state.value.loadingMore)
    }
}
