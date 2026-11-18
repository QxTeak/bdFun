package com.aritxonly.bdfun.feature.comment.application

import com.aritxonly.bdfun.core.application.CommentLikeStore
import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.comment.CommentPage
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CommentThreadStateHolderTest {
    private class Fake : CommentContentRepository {
        var postResult: Outcome<Long?> = Outcome.Success(55L)
        var deleteResult: Outcome<Unit> = Outcome.Success(Unit)
        var sent: CommentPost? = null
        var beforePost: suspend () -> Unit = {}
        var beforeDelete: suspend () -> Unit = {}
        override suspend fun page(query: CommentQuery, page: Int): Outcome<CommentPage> = error("unused")
        override suspend fun post(post: CommentPost): Outcome<Long?> { sent = post; beforePost(); return postResult }
        override suspend fun like(query: CommentQuery, rpid: Long, liked: Boolean): Outcome<Unit> = error("unused")
        override suspend fun report(query: CommentQuery, rpid: Long): Outcome<Unit> = error("unused")
        override suspend fun delete(query: CommentQuery, rpid: Long): Outcome<Unit> { beforeDelete(); return deleteResult }
        override suspend fun block(mid: Long): Outcome<Unit> = error("unused")
    }
    private fun holder(fake: Fake, root: Long = 10) = CommentThreadStateHolder(
        CommentActionsStateHolder(fake, CommentQuery("oid", 17, 1, root), CommentLikeStore()), root,
    )
    @Test fun successfulReplyRetainsRootParentAndPrependsLocalProjection() = runBlocking {
        val fake = Fake(); val holder = holder(fake)
        assertEquals(55L, holder.send("内容", 20, null) { 9000L })
        assertEquals(10L, fake.sent?.root)
        assertEquals(20L, fake.sent?.parent)
        val item = holder.state.value.extra.single()
        assertEquals(55L, item.rpid); assertEquals(10L, item.root); assertEquals(20L, item.parent)
        assertEquals("我", item.uname); assertEquals(9L, item.ctime)
    }
    @Test fun nullPostDoesNotCreatePhantomReply() = runBlocking {
        val fake = Fake().apply { postResult = Outcome.Success(null) }
        val holder = holder(fake)
        assertNull(holder.send("内容", 20, null) { 9000L })
        assertTrue(holder.state.value.extra.isEmpty())
    }
    @Test fun missingPositiveIdKeepsOriginalTimestampFallback() = runBlocking {
        val fake = Fake().apply { postResult = Outcome.Success(0L) }
        val holder = holder(fake)
        holder.send("内容", 20, null) { 9000L }
        assertEquals(9000L, holder.state.value.extra.single().rpid)
    }
    @Test fun failedDeleteDoesNotHideReplyAndOtherRootKeepsItsOwnState() = runBlocking {
        val fake = Fake().apply { deleteResult = Outcome.Failure(AppError.Network("offline")) }
        val holder = holder(fake); val other = holder(fake, 11)
        assertFalse(holder.delete(55))
        assertTrue(holder.state.value.deleted.isEmpty())
        fake.deleteResult = Outcome.Success(Unit)
        assertTrue(holder.delete(55))
        assertEquals(setOf(55L), holder.state.value.deleted)
        assertTrue(other.state.value.deleted.isEmpty())
    }

    @Test fun closedThreadRejectsLateSendWithoutLocalProjectionOrClockRead() = runBlocking {
        val gate = CompletableDeferred<Unit>(); var clockReads = 0
        val fake = Fake().apply { beforePost = { gate.await() } }
        val holder = holder(fake)
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.send("text", 20, null) { clockReads++; 9000L } }
        holder.close(); gate.complete(Unit)
        assertNull(job.await()); assertEquals(0, clockReads)
        assertTrue(holder.state.value.extra.isEmpty())
        assertNull(holder.send("later", 20, null) { 9000L }); assertFalse(holder.delete(55))
    }

    @Test fun closedThreadRejectsLateDeleteAndRetainsExistingLocalProjection() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake(); val holder = holder(fake)
        holder.send("text", 20, null) { 9000L }
        fake.beforeDelete = { gate.await() }
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.delete(55) }
        holder.close(); val closed = holder.state.value
        gate.complete(Unit); assertFalse(job.await()); assertEquals(closed, holder.state.value)
        assertEquals(55L, holder.state.value.extra.single().rpid)
        assertTrue(holder.state.value.deleted.isEmpty())
    }

    @Test fun accountInvalidationRejectsLateDeleteWithoutHidingNewAccountReply() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { beforeDelete = { gate.await() } }
        val likes = CommentLikeStore()
        val actions = CommentActionsStateHolder(fake, CommentQuery("oid", 17, 1, 10), likes)
        val holder = CommentThreadStateHolder(actions, 10)
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.delete(55) }
        likes.clear(); gate.complete(Unit)
        assertFalse(job.await()); assertTrue(holder.state.value.deleted.isEmpty())
    }
}
