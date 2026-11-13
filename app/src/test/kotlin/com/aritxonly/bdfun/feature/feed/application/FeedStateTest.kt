package com.aritxonly.bdfun.feature.feed.application

import com.aritxonly.bdfun.core.domain.dynamicFeedParams
import com.aritxonly.bdfun.core.model.feed.FeedItemAuthor
import com.aritxonly.bdfun.core.model.feed.FeedCounts
import com.aritxonly.bdfun.core.model.feed.FeedAuthor
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.common.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FeedStateTest {
    private fun item(id: String) = FeedItem(
        id = id,
        author = FeedItemAuthor(
            name = "UP",
            face = "",
            mid = 7,
        ),
        time = "",
        text = "",
        images = emptyList(),
        video = null,
        counts = FeedCounts(
            like = 0,
            comment = 0,
            forward = 0,
        ),
    )
    private fun page(vararg ids: String, offset: String = "", more: Boolean = false) =
        FeedPage(ids.map(::item), offset, more)
    private class Source : FeedPageRepository {
        val requests = mutableListOf<Triple<String, String, Long?>>()
        var feed: suspend (String, String, Long?) -> FeedPage = { _, _, _ -> FeedPage(emptyList(), "", false) }
        var portal = FeedPortal(FeedUpPage(emptyList(), "", false), emptyList(), 0)
        var ups: suspend (String) -> FeedUpPage = { FeedUpPage(emptyList(), "", false) }
        override suspend fun dynamicPage(type: String, offset: String, hostMid: Long?): Outcome<FeedPage> {
            requests += Triple(type, offset, hostMid)
            return outcome { feed(type, offset, hostMid) }
        }
        var portalGate: CompletableDeferred<FeedPortal>? = null
        var portalCalls = 0
        override suspend fun dynamicPortal(): Outcome<FeedPortal> {
            portalCalls++
            return Outcome.Success(portalGate?.await() ?: portal)
        }
        override suspend fun dynamicUpList(offset: String) = outcome { ups(offset) }
        val likes = mutableListOf<Pair<String, Boolean>>()
        var like: suspend (String, Boolean) -> Outcome<Boolean> = { _, _ -> Outcome.Success(true) }
        override suspend fun likeFeed(id: String, like: Boolean): Outcome<Boolean> { likes += id to like; return this.like(id, like) }
        private suspend fun <T> outcome(block: suspend () -> T): Outcome<T> = try { Outcome.Success(block()) }
        catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { Outcome.Failure(AppError.Unknown(failure.message.orEmpty())) }
    }

    @Test fun upRequestUsesHostMidAndNoCategoryOrFixedPage() {
        val params = dynamicFeedParams("up", "next", 42)
        assertEquals("42", params["host_mid"])
        assertEquals("next", params["offset"])
        assertFalse(params.containsKey("type"))
        assertFalse(params.containsKey("page"))
        assertTrue(params.getValue("features").contains("opusBigCover"))
        for (type in listOf("all", "video", "pgc", "article")) {
            assertEquals(type, dynamicFeedParams(type, "", null)["type"])
        }
    }

    @Test fun categoryCachesKeepServerOrderAndCursor() = runBlocking {
        val source = Source().apply { feed = { type, cursor, _ ->
            if (cursor.isEmpty()) page("$type-2", "$type-1", offset = "$type-next", more = true)
            else page("$type-1", "$type-3")
        } }
        val state = FeedStateHolder(source)
        state.ensureLoaded(true)
        state.switchTo("video", true)
        state.switchTo("all", true)
        assertEquals(2, source.requests.size)
        state.loadMore(true)
        assertEquals("all-next", source.requests.last().second)
        assertEquals(listOf("all-2", "all-1", "all-3"), state.items.map { it.id })
    }

    @Test fun lateAuthorResponseCannotOverwriteNewSelection() = runBlocking {
        val pending = CompletableDeferred<FeedPage>()
        val source = Source().apply { feed = { _, _, mid -> if (mid == 1L) pending.await() else page("new") } }
        val state = FeedStateHolder(source)
        val old = launch(start = CoroutineStart.UNDISPATCHED) { state.filterTo(1) }
        state.filterTo(2)
        pending.complete(page("old"))
        old.join()
        assertEquals(2L, state.filterMid)
        assertEquals(listOf("new"), state.items.map { it.id })
        assertFalse(state.refreshing)
    }

    @Test fun refreshSelectedAuthorKeepsHostAndResetsCursor() = runBlocking {
        val source = Source().apply { feed = { _, _, _ -> page("one", offset = "next", more = true) } }
        val state = FeedStateHolder(source)
        state.filterTo(7)
        state.loadMore(true)
        state.refresh(true)
        assertEquals(Triple("up", "", 7L), source.requests.last())
        assertEquals("up", state.type)
    }

    @Test fun duplicatePageAndStuckCursorTerminate() = runBlocking {
        val source = Source().apply { feed = { _, _, _ -> page("same", offset = "stuck", more = true) } }
        val state = FeedStateHolder(source)
        state.ensureLoaded(true)
        state.loadMore(true)
        state.loadMore(true)
        assertEquals(2, source.requests.size)
        assertEquals(1, state.items.size)
        assertFalse(state.hasMore)
    }

    @Test fun emptyIntermediatePageContinuesToUsableContent() = runBlocking {
        val source = Source().apply { feed = { _, cursor, _ ->
            if (cursor.isEmpty()) page(offset = "next", more = true) else page("found")
        } }
        val state = FeedStateHolder(source)
        state.ensureLoaded(true)
        assertEquals(listOf("found"), state.items.map { it.id })
    }

    @Test fun emptySuccessfulFeedAndLoggedOutDoNotRepeatedlyFetch() = runBlocking {
        val source = Source()
        val state = FeedStateHolder(source)
        state.ensureLoaded(false)
        assertTrue(source.requests.isEmpty())
        state.ensureLoaded(true)
        state.ensureLoaded(true)
        assertEquals(1, source.requests.size)
    }

    @Test fun failedRefreshRetainsVisibleContentAndCanRetry() = runBlocking {
        val source = Source().apply { feed = { _, _, _ -> page("cached") } }
        val state = FeedStateHolder(source)
        state.ensureLoaded(true)
        source.feed = { _, _, _ -> error("offline") }
        state.refresh(true)
        assertEquals("cached", state.items.single().id)
        assertEquals("offline", state.error)
        assertFalse(state.refreshing)
        source.feed = { _, _, _ -> page("fresh") }
        state.retry(true)
        assertNull(state.error)
        assertEquals("fresh", state.items.single().id)
    }

    @Test fun portalOrderIsIndependentOfFeedAndReadMarkerDoesNotReorder() = runBlocking {
        val source = Source().apply {
            portal = FeedPortal(FeedUpPage(listOf(
                FeedAuthor(9, "Nine", "", true), FeedAuthor(2, "Two", "", true),
            ), "up-next", true), emptyList(), 0)
            ups = { FeedUpPage(listOf(FeedAuthor(2, "Two", ""), FeedAuthor(3, "Three", "")), "up-next", true) }
        }
        val state = FeedStateHolder(source)
        state.ensureLoaded(true)
        assertTrue(state.items.isEmpty())
        state.markSeen(9)
        assertEquals(listOf(9L, 2L), state.authors.map { it.mid })
        assertFalse(state.authors.first().hasUpdate)
        state.loadMoreAuthors()
        assertEquals(listOf(9L, 2L, 3L), state.authors.map { it.mid })
        assertFalse(state.authorsHasMore)
    }

    @Test fun refreshWinsOverOutstandingPagination() = runBlocking {
        val pending = CompletableDeferred<FeedPage>()
        val source = Source().apply { feed = { _, cursor, _ ->
            if (cursor.isEmpty()) page("first", offset = "old-next", more = true) else pending.await()
        } }
        val state = FeedStateHolder(source)
        state.ensureLoaded(true)
        val load = launch(start = CoroutineStart.UNDISPATCHED) { state.loadMore(true) }
        source.feed = { _, _, _ -> page("fresh") }
        state.refresh(true)
        pending.complete(page("stale"))
        load.join()
        assertEquals(listOf("fresh"), state.items.map { it.id })
    }

    @Test fun optimisticLikeIsSerializedAndCommitsAfterAcknowledgement() = runBlocking {
        val pending = CompletableDeferred<Outcome<Boolean>>()
        val source = Source().apply { like = { _, _ -> pending.await() } }
        val state = FeedStateHolder(source)
        val item = item("like")
        val running = async(start = CoroutineStart.UNDISPATCHED) { state.toggleLike(item) }
        assertEquals(true, state.liked[item.id])
        assertEquals(1, state.likeCounts[item.id])
        assertNull(state.toggleLike(item))
        assertEquals(listOf(item.id to true), source.likes)
        pending.complete(Outcome.Success(true))
        assertEquals(Outcome.Success(true), running.await())
        assertTrue(state.state.value.interactions.pending.isEmpty())
    }
    @Test fun failedLikeAndCancellationRestoreThePreviousCount() = runBlocking {
        val source = Source().apply { like = { _, _ -> Outcome.Failure(AppError.Network("offline")) } }
        val state = FeedStateHolder(source)
        val item = item("like")
        assertTrue(state.toggleLike(item) is Outcome.Failure)
        assertEquals(false, state.liked[item.id])
        assertEquals(0, state.likeCounts[item.id])
        val pending = CompletableDeferred<Outcome<Boolean>>()
        source.like = { _, _ -> pending.await() }
        val running = async(start = CoroutineStart.UNDISPATCHED) { state.toggleLike(item) }
        running.cancelAndJoin()
        assertEquals(false, state.liked[item.id])
        assertEquals(0, state.likeCounts[item.id])
        assertTrue(state.state.value.interactions.pending.isEmpty())
    }
    @Test fun unlikeReturnsTheNewStateAndClampsTheCount() = runBlocking {
        val source = Source()
        val state = FeedStateHolder(source)
        val item = item("liked").let { it.copy(counts = it.counts.copy(liked = true, like = 0)) }
        assertEquals(Outcome.Success(false), state.toggleLike(item))
        assertEquals(listOf(item.id to false), source.likes)
        assertEquals(0, state.likeCounts[item.id])
    }

    @Test fun closedAccountRejectsLateFeedAndFutureActions() = runBlocking {
        val pending = CompletableDeferred<FeedPage>()
        val source = Source().apply { feed = { _, _, _ -> pending.await() } }
        val state = FeedStateHolder(source)
        val running = launch(start = CoroutineStart.UNDISPATCHED) { state.ensureLoaded(true) }
        state.close(); val closed = state.state.value
        pending.complete(page("old")); running.join()
        state.ensureLoaded(true); state.refresh(true); state.loadMore(true); state.retry(true)
        state.filterTo(7); state.switchTo("video", true); state.clearFilter(); state.markSeen(7)
        assertEquals(closed, state.state.value)
        assertEquals(1, source.requests.size)
        assertFalse(state.loading); assertFalse(state.refreshing)
    }

    @Test fun closedAccountRejectsLatePortalAndFurtherPortalRequests() = runBlocking {
        val source = Source().apply { portalGate = CompletableDeferred() }
        val state = FeedStateHolder(source)
        val running = launch(start = CoroutineStart.UNDISPATCHED) { state.refreshAuthors() }
        state.close(); val closed = state.state.value
        source.portalGate!!.complete(FeedPortal(FeedUpPage(listOf(FeedAuthor(1, "旧账号", "")), "next", true), emptyList(), 0))
        running.join(); state.refreshAuthors(); state.loadMoreAuthors()
        assertEquals(closed, state.state.value); assertEquals(1, source.portalCalls)
        assertFalse(state.authorsLoading)
    }

    @Test fun closedAccountRejectsLateAuthorPage() = runBlocking {
        val pending = CompletableDeferred<FeedUpPage>()
        val source = Source().apply {
            portal = FeedPortal(FeedUpPage(listOf(FeedAuthor(1, "One", "")), "next", true), emptyList(), 0)
            ups = { pending.await() }
        }
        val state = FeedStateHolder(source); state.refreshAuthors()
        val running = launch(start = CoroutineStart.UNDISPATCHED) { state.loadMoreAuthors() }
        state.close(); val closed = state.state.value
        pending.complete(FeedUpPage(listOf(FeedAuthor(2, "Two", "")), "", false)); running.join()
        assertEquals(closed, state.state.value); assertFalse(state.authorsLoading)
    }

    @Test fun closedAccountSuppressesLateLikeReplyAndNoFurtherLikeIsSent() = runBlocking {
        val pending = CompletableDeferred<Outcome<Boolean>>()
        val source = Source().apply { like = { _, _ -> pending.await() } }
        val state = FeedStateHolder(source)
        val running = async(start = CoroutineStart.UNDISPATCHED) { state.toggleLike(item("one")) }
        state.close(); val closed = state.state.value
        pending.complete(Outcome.Success(false)); assertNull(running.await())
        assertEquals(closed, state.state.value); assertNull(state.toggleLike(item("two")))
        assertEquals(1, source.likes.size); assertTrue(state.state.value.interactions.pending.isEmpty())
    }
}
