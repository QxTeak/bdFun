package com.aritxonly.bdfun.feature.comment.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.comment.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CommentListStateHolderTest {
    private val query = CommentQuery("oid", 17, 9)
    private fun item(id: Long) = CommentItem(id, "UP", "", "正文", 0, 0, 1, 0, false, false)
    private class Fake : CommentContentRepository {
        var load: suspend (CommentQuery, Int) -> Outcome<CommentPage> = { _, page -> Outcome.Success(CommentPage(emptyList(), page, true, 0)) }
        val requests = mutableListOf<Pair<CommentQuery, Int>>()
        override suspend fun page(query: CommentQuery, page: Int): Outcome<CommentPage> {
            requests += query to page; return load(query, page)
        }
        override suspend fun post(post: CommentPost): Outcome<Long?> = error("unused")
        override suspend fun like(query: CommentQuery, rpid: Long, liked: Boolean): Outcome<Unit> = error("unused")
        override suspend fun report(query: CommentQuery, rpid: Long): Outcome<Unit> = error("unused")
        override suspend fun delete(query: CommentQuery, rpid: Long): Outcome<Unit> = error("unused")
        override suspend fun block(mid: Long): Outcome<Unit> = error("unused")
    }
    @Test fun paginationDeduplicatesAndRetainsServerTotal() = runBlocking {
        val fake = Fake().apply { load = { _, page -> Outcome.Success(CommentPage(
            if (page == 1) listOf(item(1)) else listOf(item(1), item(2)), page, page == 2, 99)) } }
        val holder = CommentListStateHolder(fake, query)
        holder.reset(); holder.loadMore()
        assertEquals(listOf(1L, 2L), holder.state.value.items.map { it.rpid })
        assertEquals(99, holder.state.value.total)
        assertTrue(holder.state.value.end)
        assertEquals(listOf(1, 2), fake.requests.map { it.second })
    }
    @Test fun sortingInvalidatesOlderResponse() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { load = { q, page ->
            if (q.sortHot) gate.await()
            Outcome.Success(CommentPage(listOf(item(if (q.sortHot) 1 else 2)), page, true, 1))
        } }
        val holder = CommentListStateHolder(fake, query)
        val old = launch(start = CoroutineStart.UNDISPATCHED) { holder.reset() }
        holder.reset(query.copy(sortHot = false))
        gate.complete(Unit); old.join()
        assertEquals(2L, holder.state.value.items.single().rpid)
        assertFalse(holder.state.value.query.sortHot)
    }
    @Test fun switchingRootRejectsOldThreadResults() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { load = { q, page ->
            if (q.root == 1L) gate.await()
            Outcome.Success(CommentPage(listOf(item(q.root)), page, true, 1))
        } }
        val holder = CommentListStateHolder(fake, query)
        val old = launch(start = CoroutineStart.UNDISPATCHED) { holder.reset(query.copy(root = 1)) }
        holder.reset(query.copy(root = 2))
        gate.complete(Unit); old.join()
        assertEquals(2L, holder.state.value.items.single().rpid)
    }
    @Test fun cancelledPaginationCanBeRetriedWithoutInventingError() = runBlocking {
        val fake = Fake().apply { load = { _, page -> Outcome.Success(CommentPage(listOf(item(1)), page, false, 9)) } }
        val holder = CommentListStateHolder(fake, query); holder.reset()
        fake.load = { _, _ -> awaitCancellation() }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadMore() }
        job.cancelAndJoin()
        assertFalse(holder.state.value.loadingMore)
        assertNull(holder.state.value.error)
        assertEquals(1, holder.state.value.page)
    }
    @Test fun failedPageKeepsVisibleRowsAndCursor() = runBlocking {
        val fake = Fake().apply { load = { _, page -> Outcome.Success(CommentPage(listOf(item(1)), page, false, 9)) } }
        val holder = CommentListStateHolder(fake, query); holder.reset()
        fake.load = { _, _ -> Outcome.Failure(AppError.Network("offline")) }
        holder.loadMore()
        assertEquals(1L, holder.state.value.items.single().rpid)
        assertEquals(1, holder.state.value.page)
        assertFalse(holder.state.value.loadingMore)
    }
    @Test fun sentCommentIsPinnedFirstUntilTheNextServerReload() = runBlocking {
        val fake = Fake().apply { load = { _, page -> Outcome.Success(CommentPage(listOf(item(1)), page, true, 9)) } }
        val holder = CommentListStateHolder(fake, query); holder.reset()
        holder.prependLocal(item(77))
        assertEquals(listOf(77L, 1L), holder.state.value.items.map { it.rpid })
        assertEquals(10, holder.state.value.total)
        holder.reload()
        assertEquals(listOf(1L), holder.state.value.items.map { it.rpid })
        assertEquals(9, holder.state.value.total)
    }

    @Test fun likeProjectionUpdatesCollapsedPreviewAsWellAsTopLevel() = runBlocking {
        val fake = Fake().apply { load = { _, page -> Outcome.Success(CommentPage(
            listOf(item(1).let { it.copy(extras = it.extras.copy(previewReplies = listOf(item(2)))) }), page, true, 1)) } }
        val holder = CommentListStateHolder(fake, query); holder.reset()
        holder.applyLike(2, true, 8)
        assertTrue(holder.state.value.items.single().previewReplies.single().liked)
        assertEquals(8, holder.state.value.items.single().previewReplies.single().like)
    }

    @Test fun viewportCallbackReadsCurrentPageAndFlagsAcrossThreePages() = runBlocking {
        val fake = Fake().apply { load = { _, page -> Outcome.Success(CommentPage(listOf(item(page.toLong())), page, page == 3, 99)) } }
        val holder = CommentListStateHolder(fake, query)
        val viewport = { holder.shouldLoadMore(9, 10) }
        assertFalse(viewport()); holder.reset()
        assertFalse(holder.shouldLoadMore(8, 10)); assertTrue(viewport())
        holder.loadMore(); assertTrue(viewport())
        holder.loadMore(); assertFalse(viewport())
        assertEquals(listOf(1, 2, 3), fake.requests.map { it.second })
        assertEquals(listOf(1L, 2L, 3L), holder.state.value.items.map { it.rpid })
    }

    @Test fun viewportBusyAndCancelAreReadLiveIncludingOriginalZeroLayoutCountBoundary() = runBlocking {
        val fake = Fake().apply { load = { _, page -> Outcome.Success(CommentPage(listOf(item(1)), page, false, 99)) } }
        val holder = CommentListStateHolder(fake, query); holder.reset()
        assertTrue(holder.shouldLoadMore(-1, 0))
        fake.load = { _, _ -> awaitCancellation() }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadMore() }
        assertFalse(holder.shouldLoadMore(9, 10))
        job.cancelAndJoin(); assertTrue(holder.shouldLoadMore(9, 10))
    }

    @Test fun closedListRejectsLatePageAndAllFutureMutations() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { load = { _, page -> gate.await(); Outcome.Success(CommentPage(listOf(item(2)), page, false, 99)) } }
        val holder = CommentListStateHolder(fake, query)
        val old = launch(start = CoroutineStart.UNDISPATCHED) { holder.reload() }
        holder.close(); val closed = holder.state.value
        assertFalse(closed.loading); assertFalse(closed.loadingMore)
        gate.complete(Unit); old.join()
        holder.reset(query.copy(root = 8)); holder.reload(); holder.loadMore()
        holder.applyLike(2, true, 10); holder.remove(2); holder.close()
        assertEquals(closed, holder.state.value)
        assertFalse(holder.shouldLoadMore(9, 10)); assertEquals(1, fake.requests.size)
    }

    @Test fun closeDuringPaginationReleasesBusyAndRetainsVisibleRows() = runBlocking {
        val fake = Fake().apply { load = { _, page -> Outcome.Success(CommentPage(listOf(item(1)), page, false, 9)) } }
        val holder = CommentListStateHolder(fake, query); holder.reset()
        val gate = CompletableDeferred<Unit>()
        fake.load = { _, page -> gate.await(); Outcome.Success(CommentPage(listOf(item(2)), page, true, 9)) }
        val old = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadMore() }
        holder.close(); val closed = holder.state.value
        assertFalse(closed.loadingMore); assertEquals(1, closed.page)
        gate.complete(Unit); old.join(); assertEquals(closed, holder.state.value)
    }

    @Test fun cancellationSwallowedByRepositoryCannotPublishPage() = runBlocking {
        val fake = Fake().apply { load = { _, page ->
            try { awaitCancellation() } catch (_: CancellationException) { }
            Outcome.Success(CommentPage(listOf(item(1)), page, true, 1))
        } }
        val holder = CommentListStateHolder(fake, query)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.reset() }
        job.cancelAndJoin()
        assertTrue(holder.state.value.items.isEmpty()); assertFalse(holder.state.value.loading)
    }
}
