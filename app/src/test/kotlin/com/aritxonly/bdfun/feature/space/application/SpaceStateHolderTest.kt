package com.aritxonly.bdfun.feature.space.application

import com.aritxonly.bdfun.core.model.feed.FeedItemAuthor

import com.aritxonly.bdfun.core.model.feed.FeedCounts

import com.aritxonly.bdfun.core.model.feed.FeedAuthor

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.SpaceRepository
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.feed.FeedPage
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SpaceStateHolderTest {
    private fun video(id: Long) = VideoItem("BV$id", id, "标题", "", "UP", "", 1, 0, 0, 1, 0)
    private fun dynamic(id: String) = FeedItem(
        id = id,
        author = FeedItemAuthor(
            name = "UP",
            face = "",
            mid = 1,
        ),
        time = "时间",
        text = "正文",
        images = emptyList(),
        video = null,
        counts = FeedCounts(
            like = 0,
            comment = 0,
            forward = 0,
        ),
    )
    private class Fake : SpaceRepository {
        var profile: Outcome<SpaceInfo> = Outcome.Success(SpaceInfo(UserProfile(1, "姓名", "", "", 1, 0.0, false, ""), emptyList()))
        var videos = emptyList<VideoItem>()
        var hook: suspend (String) -> Unit = {}
        val requests = mutableListOf<String>()
        suspend fun visit(name: String) { requests += name; hook(name) }
        var dynamicResult: Outcome<FeedPage> = Outcome.Success(FeedPage(emptyList(), "", false))
        var relation: Int? = null
        var followResult: Outcome<Boolean> = Outcome.Success(true)
        var followGate: CompletableDeferred<Unit>? = null
        var collectionGate: CompletableDeferred<Unit>? = null
        var videosGate: CompletableDeferred<Unit>? = null
        override suspend fun profile(mid: Long): Outcome<SpaceInfo> { visit("profile"); return profile }
        override suspend fun seasons(mid: Long): Outcome<List<SpaceSeason>> { visit("seasons"); return Outcome.Success(emptyList()) }
        override suspend fun dynamics(mid: Long, offset: String): Outcome<FeedPage> { visit("dynamics"); return dynamicResult }
        override suspend fun relation(mid: Long): Outcome<Int?> { visit("relation"); return Outcome.Success(relation) }
        override suspend fun videos(mid: Long, page: Int): Outcome<List<VideoItem>> {
            visit("videos:$page"); videosGate?.await()
            return Outcome.Success(videos)
        }
        override suspend fun articles(mid: Long, page: Int): Outcome<List<SpaceArticle>> { visit("articles:$page"); return Outcome.Success(emptyList()) }
        override suspend fun bangumi(mid: Long, page: Int): Outcome<List<SpaceBangumi>> { visit("bangumi:$page"); return Outcome.Success(emptyList()) }
        override suspend fun shop(mid: Long): Outcome<List<SpaceShopItem>> { visit("shop"); return Outcome.Success(emptyList()) }
        override suspend fun collection(mid: Long, collection: SpaceSeason): Outcome<List<VideoItem>> {
            visit("collection"); if (collection.id == 1L) collectionGate?.await()
            return Outcome.Success(videos)
        }
        override suspend fun follow(mid: Long, followed: Boolean): Outcome<Boolean> { visit("follow"); followGate?.await(); return followResult }
    }

    @Test fun bootstrapFallsBackToSeparateVideoFeedAndResolvesMutualFollow() = runBlocking {
        val source = Fake(); source.videos = listOf(video(1)); source.relation = 6
        val holder = SpaceStateHolder(1, source); holder.load(false, true)
        assertEquals(source.videos, holder.state.value.videos.items)
        assertTrue(holder.state.value.profile.followed)
        assertFalse(holder.state.value.profile.loading)
        assertTrue(holder.state.value.profile.settled)
    }

    @Test fun includedVideosKeepOriginalShortPageEndRule() = runBlocking {
        val source = Fake()
        source.profile = Outcome.Success((source.profile as Outcome.Success).value.copy(videos = listOf(video(1)), isFollowed = true))
        source.videos = listOf(video(2))
        val holder = SpaceStateHolder(1, source); holder.load(false, false)
        assertEquals(listOf(video(1)), holder.state.value.videos.items)
        assertTrue(holder.state.value.videos.end)
        assertTrue(holder.state.value.profile.followed)
    }

    @Test fun profileFailureExposesErrorAndSettlesLoading() = runBlocking {
        val source = Fake(); source.profile = Outcome.Failure(AppError.Network("offline"))
        val holder = SpaceStateHolder(1, source); holder.load(false, false)
        assertEquals("offline", holder.state.value.profile.error)
        assertNull(holder.state.value.profile.space)
        assertFalse(holder.state.value.profile.loading)
    }

    @Test fun failedAndCancelledFollowsRestorePreviousState() = runBlocking {
        val source = Fake(); val holder = SpaceStateHolder(1, source)
        source.followResult = Outcome.Success(false)
        assertEquals("操作失败", holder.toggleFollow())
        assertFalse(holder.state.value.profile.followed)
        source.followGate = CompletableDeferred()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.toggleFollow() }
        assertTrue(holder.state.value.profile.followed)
        assertNull(holder.toggleFollow())
        job.cancelAndJoin()
        assertFalse(holder.state.value.profile.followed)
        assertFalse(holder.state.value.profile.followBusy)
    }

    @Test fun lateCollectionResponseCannotReplaceNewerSelection() = runBlocking {
        val source = Fake(); source.collectionGate = CompletableDeferred()
        val holder = SpaceStateHolder(1, source)
        val first = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadCollection(SpaceSeason(1, "一", "")) }
        source.videos = listOf(video(2)); holder.loadCollection(SpaceSeason(2, "二", ""))
        source.videos = listOf(video(1)); source.collectionGate!!.complete(Unit); first.join()
        assertEquals(listOf(video(2)), holder.state.value.collection.items)
        assertFalse(holder.state.value.collection.loading)
    }

    @Test fun nextVideoRequestReadsTheLatestCompletedPage() = runBlocking {
        val source = Fake(); source.videos = listOf(video(1))
        val holder = SpaceStateHolder(1, source)
        assertEquals(SpacePageRequest.Videos(2), holder.nextPage(2, 0))
        holder.loadVideos(2)
        assertEquals(SpacePageRequest.Videos(3), holder.nextPage(2, 0))
        source.videos = listOf(video(2)); holder.loadVideos(3)
        assertEquals(SpacePageRequest.Videos(4), holder.nextPage(2, 0))
    }

    @Test fun activeRequestSuppressesPagingAndCancellationRestoresIt() = runBlocking {
        val source = Fake(); source.videosGate = CompletableDeferred()
        val holder = SpaceStateHolder(1, source)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadVideos(2) }
        assertNull(holder.nextPage(2, 0))
        job.cancelAndJoin()
        assertEquals(SpacePageRequest.Videos(2), holder.nextPage(2, 0))
    }

    @Test fun reachingVideoEndStopsFurtherRequests() = runBlocking {
        val holder = SpaceStateHolder(1, Fake())
        holder.loadVideos(2)
        assertNull(holder.nextPage(2, 0))
    }

    @Test fun collectionAndOverviewModesDoNotPageTheVideoFeed() {
        val holder = SpaceStateHolder(1, Fake())
        assertNull(holder.nextPage(0, 0))
        assertNull(holder.nextPage(2, 2))
        assertNull(holder.nextPage(2, 3))
        assertNull(holder.nextPage(4, 0))
    }

    @Test fun articleAndOriginalFourthTabRequestsKeepSeparateEndStates() = runBlocking {
        val holder = SpaceStateHolder(1, Fake())
        assertEquals(SpacePageRequest.Articles(2), holder.nextPage(2, 1))
        assertEquals(SpacePageRequest.Bangumi(2), holder.nextPage(3, 0))
        holder.loadArticles(2)
        assertNull(holder.nextPage(2, 1))
        assertEquals(SpacePageRequest.Bangumi(2), holder.nextPage(3, 0))
        holder.loadBangumi(2)
        assertNull(holder.nextPage(3, 0))
    }

    @Test fun dynamicEndIsReadAtTheNextScrollEvent() = runBlocking {
        val holder = SpaceStateHolder(1, Fake())
        assertEquals(SpacePageRequest.Feeds, holder.nextPage(1, 0))
        holder.loadMoreFeeds()
        assertNull(holder.nextPage(1, 0))
    }

    @Test fun closeRejectsLateBootstrapAtEveryAwaitStage() = runBlocking {
        for (stage in listOf("profile", "seasons", "dynamics", "relation", "videos:1")) {
            val source = Fake(); source.videos = listOf(video(9))
            val entered = CompletableDeferred<Unit>(); val gate = CompletableDeferred<Unit>()
            source.hook = { if (it == stage) { entered.complete(Unit); gate.await() } }
            val holder = SpaceStateHolder(1, source)
            val job = launch { holder.load(false, true) }; entered.await()
            holder.close(); val stopped = holder.state.value; gate.complete(Unit); job.join()
            assertTrue(stage, job.isCancelled); assertEquals(stage, stopped, holder.state.value)
            assertFalse(holder.state.value.profile.loading); assertFalse(holder.state.value.profile.followBusy)
        }
    }
    @Test fun closeRejectsEveryLateContentResponseAndReleasesFlags() = runBlocking {
        for (stage in listOf("videos:2", "articles:2", "bangumi:2", "shop", "collection", "dynamics")) {
            val source = Fake(); source.videos = listOf(video(9))
            val entered = CompletableDeferred<Unit>(); val gate = CompletableDeferred<Unit>()
            source.hook = { if (it == stage) { entered.complete(Unit); gate.await() } }
            val holder = SpaceStateHolder(1, source)
            val job = launch { when (stage) {
                "videos:2" -> holder.loadVideos(2)
                "articles:2" -> holder.loadArticles(2)
                "bangumi:2" -> holder.loadBangumi(2)
                "shop" -> holder.loadShop()
                "collection" -> holder.loadCollection(SpaceSeason(1, "一", ""))
                else -> holder.loadMoreFeeds()
            } }
            entered.await(); holder.close(); val stopped = holder.state.value
            gate.complete(Unit); job.join()
            assertTrue(stage, job.isCancelled); assertEquals(stage, stopped, holder.state.value)
            assertNull(holder.nextPage(2, 0))
        }
    }
    @Test fun closedPageCannotStartAnyNewReadOrFollow() = runBlocking {
        val source = Fake(); val holder = SpaceStateHolder(1, source); holder.close(); holder.close()
        holder.load(false, true); holder.loadVideos(1); holder.loadArticles(1); holder.loadBangumi(1)
        holder.loadShop(); holder.loadCollection(SpaceSeason(1, "一", "")); holder.loadMoreFeeds()
        assertNull(holder.toggleFollow()); assertNull(holder.nextPage(1, 0)); assertTrue(source.requests.isEmpty())
    }
    @Test fun closedFollowRestoresOptimisticValueAndRejectsLateSuccess() = runBlocking {
        val source = Fake(); source.followGate = CompletableDeferred()
        val holder = SpaceStateHolder(1, source)
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.toggleFollow() }
        assertTrue(holder.state.value.profile.followed)
        holder.close(); val stopped = holder.state.value; source.followGate!!.complete(Unit)
        try { job.await(); fail("closed follow must cancel") } catch (_: CancellationException) { }
        assertEquals(stopped, holder.state.value); assertFalse(stopped.profile.followed); assertFalse(stopped.profile.followBusy)
    }
    @Test fun accountRoundTripInvalidatesOldPageEvenWhenMidReturns() = runBlocking {
        var revision = 1; val initial = revision
        val source = Fake(); source.videosGate = CompletableDeferred(); source.videos = listOf(video(9))
        val holder = SpaceStateHolder(1, source) { revision == initial }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadVideos(1) }
        revision = 2; revision = 3
        source.videosGate!!.complete(Unit); job.join()
        assertTrue(job.isCancelled); assertTrue(holder.state.value.videos.items.isEmpty())
        assertFalse(holder.state.value.videos.loading); assertNull(holder.nextPage(2, 0))
        val count = source.requests.size; holder.loadVideos(1); assertEquals(count, source.requests.size)
    }
    @Test fun swallowedCancellationStillCannotPublishOrAdvancePage() = runBlocking {
        val source = Fake(); source.videos = listOf(video(9))
        source.hook = { if (it == "videos:2") try { awaitCancellation() } catch (_: CancellationException) {} }
        val holder = SpaceStateHolder(1, source)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadVideos(2) }; job.cancelAndJoin()
        assertTrue(holder.state.value.videos.items.isEmpty()); assertEquals(1, holder.state.value.videos.page)
        assertFalse(holder.state.value.videos.loading)
    }
    @Test fun ordinaryRetryAfterCancellationKeepsOriginalPageAndMergeRules() = runBlocking {
        val source = Fake(); source.videosGate = CompletableDeferred(); source.videos = listOf(video(1))
        val holder = SpaceStateHolder(1, source)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadVideos(2) }; job.cancelAndJoin()
        source.videosGate = null; holder.loadVideos(2)
        source.videos = listOf(video(1), video(2)); holder.loadVideos(3)
        assertEquals(listOf(video(1), video(2)), holder.state.value.videos.items); assertEquals(3, holder.state.value.videos.page)
    }

    @Test fun bootstrapFeedFailureKeepsInitialPageRetryableAsInMain() = runBlocking {
        val source = Fake(); source.dynamicResult = Outcome.Failure(AppError.Network("temporary"))
        val holder = SpaceStateHolder(1, source); holder.load(false, false)
        assertFalse(holder.state.value.dynamics.end); assertEquals("", holder.state.value.dynamics.offset)
        assertNull(holder.state.value.dynamics.error); assertEquals(SpacePageRequest.Feeds, holder.nextPage(1, 0))
        source.dynamicResult = Outcome.Success(FeedPage(listOf(dynamic("retry")), "next", true))
        holder.loadMoreFeeds()
        assertEquals("next", holder.state.value.dynamics.offset); assertFalse(holder.state.value.dynamics.end)
        assertEquals(listOf(dynamic("retry")), holder.state.value.dynamics.items)
        assertEquals(2, source.requests.count { it == "dynamics" })
    }
    @Test fun failedBootstrapRefreshKeepsPreviouslyLoadedFeedCursorAndItems() = runBlocking {
        val source = Fake(); source.dynamicResult = Outcome.Success(FeedPage(listOf(dynamic("kept")), "kept", true))
        val holder = SpaceStateHolder(1, source); holder.load(false, false)
        val original = holder.state.value.dynamics
        source.dynamicResult = Outcome.Failure(AppError.Network("temporary")); holder.load(false, false)
        assertEquals(original, holder.state.value.dynamics)
    }
    @Test fun dynamicPaginationFailureStillStopsFurtherAutomaticRequestsAsInMain() = runBlocking {
        val source = Fake(); source.dynamicResult = Outcome.Failure(AppError.Network("temporary"))
        val holder = SpaceStateHolder(1, source); holder.loadMoreFeeds()
        assertTrue(holder.state.value.dynamics.end); assertNull(holder.nextPage(1, 0))
        holder.loadMoreFeeds(); assertEquals(1, source.requests.count { it == "dynamics" })
    }
}
