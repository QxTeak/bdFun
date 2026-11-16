package com.aritxonly.bdfun.feature.comment.application

import com.aritxonly.bdfun.core.application.CommentLikeStore
import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.comment.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CommentActionsStateHolderTest {
    private val query = CommentQuery("oid", 17, 1)
    private val item = CommentItem(1, "UP", "", "正文", 10, 0, 1, 0, false, false)
    private class Fake : CommentContentRepository {
        var likeAction: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
        var postAction: suspend () -> Outcome<Long?> = { Outcome.Success(99L) }
        var likes = 0
        var posts = 0
        var mutations = 0
        var mutation: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
        override suspend fun page(query: CommentQuery, page: Int): Outcome<CommentPage> = error("unused")
        override suspend fun post(post: CommentPost): Outcome<Long?> { posts++; return postAction() }
        override suspend fun like(query: CommentQuery, rpid: Long, liked: Boolean): Outcome<Unit> { likes++; return likeAction() }
        override suspend fun report(query: CommentQuery, rpid: Long): Outcome<Unit> { mutations++; return mutation() }
        override suspend fun delete(query: CommentQuery, rpid: Long): Outcome<Unit> { mutations++; return mutation() }
        override suspend fun block(mid: Long): Outcome<Unit> { mutations++; return mutation() }
    }
    @Test fun cancelledLikeRestoresOriginalCountAndUnlocks() = runBlocking {
        val likes = CommentLikeStore()
        val fake = Fake().apply { likeAction = { awaitCancellation() } }
        val holder = CommentActionsStateHolder(fake, query, likes)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.toggleLike(item) }
        assertEquals(11, likes.state.value[1]?.count)
        job.cancelAndJoin()
        assertEquals(10, likes.state.value[1]?.count)
        assertFalse(likes.state.value[1]!!.liked)
        assertTrue(holder.state.value.pending.isEmpty())
    }
    @Test fun repeatedLikeOnlySendsOneRequest() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { likeAction = { gate.await(); Outcome.Success(Unit) } }
        val holder = CommentActionsStateHolder(fake, query, CommentLikeStore())
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.toggleLike(item) }
        assertNull(holder.toggleLike(item)); assertEquals(1, fake.likes)
        gate.complete(Unit); job.join()
    }
    @Test fun cancelledPostReleasesSending() = runBlocking {
        val fake = Fake().apply { postAction = { awaitCancellation() } }
        val holder = CommentActionsStateHolder(fake, query, CommentLikeStore())
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.post("text", 0, 0) }
        assertTrue(holder.state.value.sending)
        job.cancelAndJoin()
        assertFalse(holder.state.value.sending)
    }
    @Test fun accountChangeRejectsOldPostResult() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { postAction = { gate.await(); Outcome.Success(99L) } }
        val likes = CommentLikeStore()
        val holder = CommentActionsStateHolder(fake, query, likes)
        val result = async(start = CoroutineStart.UNDISPATCHED) { holder.post("text", 0, 0) }
        likes.clear(); gate.complete(Unit)
        assertNull(result.await())
    }
    @Test fun nullablePostIdIsNotInventedAsSuccessfulComment() = runBlocking {
        val holder = CommentActionsStateHolder(Fake().apply { postAction = { Outcome.Success(null) } }, query, CommentLikeStore())
        assertNull(holder.post("text", 0, 0))
    }

    @Test fun accountInvalidationPreventsOldControllerStartingNewMutations() = runBlocking {
        val fake = Fake(); val likes = CommentLikeStore()
        val holder = CommentActionsStateHolder(fake, query, likes)
        likes.clear()
        assertFalse(holder.isActive)
        assertNull(holder.toggleLike(item)); assertNull(holder.post("text", 0, 0))
        assertFalse(holder.report(1)); assertFalse(holder.delete(1)); assertFalse(holder.block(1))
        assertEquals(0, fake.likes); assertEquals(0, fake.posts); assertEquals(0, fake.mutations)
        assertTrue(likes.state.value.isEmpty())
    }

    @Test fun closeRestoresPendingLikeOnceAndLateCancellationCannotOverwriteNewPage() = runBlocking {
        val fake = Fake().apply { likeAction = { awaitCancellation() } }
        val likes = CommentLikeStore(); val holder = CommentActionsStateHolder(fake, query, likes)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.toggleLike(item) }
        assertEquals(11, likes.state.value[1]?.count)
        holder.close(); assertEquals(10, likes.state.value[1]?.count)
        likes.put(1, true, 40)
        job.cancelAndJoin(); holder.close()
        assertEquals(40, likes.state.value[1]?.count)
        assertFalse(holder.isActive); assertEquals(CommentActionState(), holder.state.value)
    }

    @Test fun closeDuringLikeRejectsLateSuccessAndFailureWithoutRewritingSharedSnapshot() = runBlocking {
        for (response in listOf(Outcome.Success(Unit), Outcome.Failure(AppError.Network("offline")))) {
            val gate = CompletableDeferred<Unit>()
            val fake = Fake().apply { likeAction = { gate.await(); response } }
            val likes = CommentLikeStore(); val holder = CommentActionsStateHolder(fake, query, likes)
            val result = async(start = CoroutineStart.UNDISPATCHED) { holder.toggleLike(item) }
            holder.close(); likes.put(1, true, 40)
            gate.complete(Unit); assertNull(result.await())
            assertEquals(40, likes.state.value[1]?.count)
            assertTrue(holder.state.value.pending.isEmpty())
        }
    }

    @Test fun closingOldAccountDoesNotRollbackNewAccountSnapshot() = runBlocking {
        val fake = Fake().apply { likeAction = { awaitCancellation() } }
        val likes = CommentLikeStore(); val holder = CommentActionsStateHolder(fake, query, likes)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.toggleLike(item) }
        likes.clear(); likes.put(1, true, 50)
        holder.close(); job.cancelAndJoin()
        assertEquals(50, likes.state.value[1]?.count)
    }

    @Test fun closedPostReleasesBusyAndRejectsLateSuccessAndFutureRequests() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { postAction = { gate.await(); Outcome.Success(99L) } }
        val holder = CommentActionsStateHolder(fake, query, CommentLikeStore())
        val result = async(start = CoroutineStart.UNDISPATCHED) { holder.post("text", 0, 0) }
        holder.close(); assertFalse(holder.state.value.sending)
        gate.complete(Unit); assertNull(result.await())
        assertNull(holder.post("later", 0, 0)); assertEquals(1, fake.posts)
        assertFalse(holder.report(1)); assertFalse(holder.delete(1)); assertFalse(holder.block(1))
        assertEquals(0, fake.mutations)
    }

    @Test fun lateModerationResultsAreSuppressedOnCloseAndAccountChange() = runBlocking {
        for (invalidateAccount in listOf(false, true)) {
            val gate = CompletableDeferred<Unit>()
            val fake = Fake().apply { mutation = { gate.await(); Outcome.Success(Unit) } }
            val likes = CommentLikeStore(); val holder = CommentActionsStateHolder(fake, query, likes)
            val jobs = listOf(async(start = CoroutineStart.UNDISPATCHED) { holder.report(1) },
                async(start = CoroutineStart.UNDISPATCHED) { holder.delete(1) },
                async(start = CoroutineStart.UNDISPATCHED) { holder.block(1) })
            if (invalidateAccount) likes.clear() else holder.close()
            gate.complete(Unit)
            assertEquals(listOf(false, false, false), jobs.awaitAll())
        }
    }

    @Test fun cancellationSwallowedByTransportStillPropagatesAndRollsBackLike() = runBlocking {
        val fake = Fake().apply { likeAction = {
            try { awaitCancellation() } catch (_: CancellationException) { }
            Outcome.Success(Unit)
        } }
        val likes = CommentLikeStore(); val holder = CommentActionsStateHolder(fake, query, likes)
        var returned = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.toggleLike(item); returned = true }
        job.cancelAndJoin()
        assertFalse(returned); assertEquals(10, likes.state.value[1]?.count)
        assertTrue(holder.state.value.pending.isEmpty())
    }
    @Test fun recreatedControllerUsesNewAccountGenerationWhileOldControllerRemainsInactive() = runBlocking {
        val fake = Fake(); val likes = CommentLikeStore()
        val old = CommentActionsStateHolder(fake, query, likes)
        likes.clear()
        val fresh = CommentActionsStateHolder(fake, query, likes)
        assertNull(old.toggleLike(item)); assertEquals("点赞成功", fresh.toggleLike(item))
        assertFalse(old.isActive); assertTrue(fresh.isActive)
        assertEquals(1, fake.likes); assertEquals(11, likes.state.value[1]?.count)
    }

    @Test fun sendTimeoutPublishesFeedbackAndAllowsRetry() = runBlocking {
        val fake = Fake().apply { postAction = { awaitCancellation() } }
        val holder = CommentActionsStateHolder(fake, query, CommentLikeStore(), sendTimeoutMs = 10)
        assertNull(holder.post("text", 1, 2))
        assertFalse(holder.state.value.sending)
        assertTrue(holder.state.value.sendError is AppError.Network)
        fake.postAction = { Outcome.Success(99L) }
        assertEquals(99L, holder.post("retry", 1, 2))
        assertNull(holder.state.value.sendError)
    }
}
