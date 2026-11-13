package com.aritxonly.bdfun.feature.feed.application

import com.aritxonly.bdfun.core.model.feed.FeedItemAuthor

import com.aritxonly.bdfun.core.model.feed.FeedCounts

import com.aritxonly.bdfun.core.model.feed.FeedAuthor

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.FeedDetailRepository
import com.aritxonly.bdfun.core.model.feed.FeedItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class FeedDetailStateHolderTest {
    private val initial = FeedItem(
        id = "one",
        author = FeedItemAuthor(
            name = "作者",
            face = "",
            mid = 1,
        ),
        time = "今天",
        text = "摘要",
        images = emptyList(),
        video = null,
        counts = FeedCounts(
            like = 2,
            comment = 3,
            forward = 4,
        ),
    )
    private class Fake : FeedDetailRepository {
        var full: Outcome<FeedItem?> = Outcome.Success(null)
        var ip: Outcome<String> = Outcome.Success("")
        var liked: Outcome<Boolean> = Outcome.Success(true)
        var detailGate: CompletableDeferred<Unit>? = null
        var ipGate: CompletableDeferred<Unit>? = null
        var likeGate: CompletableDeferred<Unit>? = null
        var detailIds = mutableListOf<String>()
        var ipMids = mutableListOf<Long>()
        var likes = mutableListOf<Pair<String, Boolean>>()
        override suspend fun detail(id: String): Outcome<FeedItem?> {
            detailIds += id; val result = full; detailGate?.await(); return result
        }
        override suspend fun userIpLocation(mid: Long): Outcome<String> {
            ipMids += mid; val result = ip; ipGate?.await(); return result
        }
        override suspend fun like(id: String, liked: Boolean): Outcome<Boolean> {
            likes += id to liked; val result = this.liked; likeGate?.await(); return result
        }
    }

    @Test fun fullDetailReplacesSummaryAndInteractionSnapshot() = runBlocking {
        val source = Fake(); val full = initial.copy(
            text = "完整正文",
            counts = initial.counts.copy(
                liked = true,
                like = 20,
            ),
            images = listOf("image"),
        )
        source.full = Outcome.Success(full)
        val holder = FeedDetailStateHolder(source, initial)
        assertEquals(FeedDetailUiState(initial), holder.state.value)
        holder.load()
        assertEquals(listOf("one"), source.detailIds)
        assertEquals(FeedDetailUiState(full), holder.state.value)
    }

    @Test fun missingOrFailedDetailPreservesSummary() = runBlocking {
        val source = Fake(); val holder = FeedDetailStateHolder(source, initial)
        holder.load(); assertEquals(initial, holder.state.value.detail)
        source.full = Outcome.Failure(AppError.Network("offline")); holder.load()
        assertEquals(FeedDetailUiState(initial), holder.state.value)
    }

    @Test fun ipLookupOnlyRunsForPositiveAuthorAndBlankLocation() = runBlocking {
        val source = Fake(); source.ip = Outcome.Success("IP属地：广东")
        val holder = FeedDetailStateHolder(source, initial); holder.loadIpLocation(); holder.loadIpLocation()
        assertEquals(listOf(1L), source.ipMids)
        assertEquals("IP属地：广东", holder.state.value.detail.ipLocation)
        FeedDetailStateHolder(source, initial.copy(
            author = initial.author.copy(
                mid = 0,
            ),
        )).loadIpLocation()
        assertEquals(1, source.ipMids.size)
    }

    @Test fun ipFailureOrBlankResultDoesNotReplaceExistingFields() = runBlocking {
        val source = Fake(); val holder = FeedDetailStateHolder(source, initial)
        source.ip = Outcome.Success("  "); holder.loadIpLocation()
        source.ip = Outcome.Failure(AppError.Network("offline")); holder.loadIpLocation()
        assertEquals(FeedDetailUiState(initial), holder.state.value)
    }

    @Test fun lateIpMergesIntoCurrentFullDetailInsteadOfRestoringSummary() = runBlocking {
        val source = Fake(); source.ip = Outcome.Success("广东"); source.ipGate = CompletableDeferred()
        val holder = FeedDetailStateHolder(source, initial)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadIpLocation() }
        val full = initial.copy(
            text = "完整正文",
            counts = initial.counts.copy(
                like = 50,
                liked = true,
            ),
        )
        source.full = Outcome.Success(full); holder.load()
        source.ipGate!!.complete(Unit); job.join()
        assertEquals(FeedDetailUiState(full.copy(
            author = full.author.copy(
                ipLocation = "广东",
            ),
        )), holder.state.value)
    }

    @Test fun lateIpCannotOverwriteOtherAuthorOrExplicitLocation() = runBlocking {
        for (full in listOf(initial.copy(
            author = initial.author.copy(
                mid = 2,
            ),
        ), initial.copy(
            author = initial.author.copy(
                ipLocation = "上海",
            ),
        ))) {
            val source = Fake(); source.ip = Outcome.Success("广东"); source.ipGate = CompletableDeferred()
            val holder = FeedDetailStateHolder(source, initial)
            val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadIpLocation() }
            source.full = Outcome.Success(full); holder.load()
            source.ipGate!!.complete(Unit); job.join()
            assertEquals(FeedDetailUiState(full), holder.state.value)
        }
    }

    @Test fun likeUpdatesBeforeSendAndSuccessfulReplyKeepsOptimisticCount() = runBlocking {
        val source = Fake(); val holder = FeedDetailStateHolder(source, initial)
        val next = holder.beginLike()!!
        assertTrue(next); assertTrue(source.likes.isEmpty())
        assertEquals(3, holder.state.value.likeCount)
        assertEquals("点赞成功", holder.commitLike(next))
        assertEquals(listOf("one" to true), source.likes)
        assertEquals(3, holder.state.value.likeCount)
        assertEquals("已取消赞", holder.commitLike(holder.beginLike()!!))
        assertFalse(holder.state.value.liked); assertEquals(2, holder.state.value.likeCount)
    }

    @Test fun unlikeRetainsOriginalUnclampedCounterAndFailureReversesDelta() = runBlocking {
        val source = Fake(); source.liked = Outcome.Success(false)
        val holder = FeedDetailStateHolder(source, initial.copy(
            counts = initial.counts.copy(
                liked = true,
                like = 0,
            ),
        ))
        val next = holder.beginLike()!!
        assertFalse(next); assertEquals(-1, holder.state.value.likeCount)
        assertEquals("点赞失败", holder.commitLike(next))
        assertTrue(holder.state.value.liked); assertEquals(0, holder.state.value.likeCount)
    }

    @Test fun concurrentClicksKeepOriginalRelativeRollbackInsteadOfRestoringFirstSnapshot() = runBlocking {
        val source = Fake(); source.liked = Outcome.Failure(AppError.Network("offline")); source.likeGate = CompletableDeferred()
        val holder = FeedDetailStateHolder(source, initial)
        val first = holder.beginLike()!!
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.commitLike(first) }
        val second = holder.beginLike()!!
        assertFalse(second); assertEquals(2, holder.state.value.likeCount)
        source.likeGate!!.complete(Unit)
        assertEquals("点赞失败", job.await())
        assertFalse(holder.state.value.liked); assertEquals(1, holder.state.value.likeCount)
    }

    @Test fun fullSnapshotDuringLikeUsesCurrentCountForFailureRollback() = runBlocking {
        val source = Fake(); source.liked = Outcome.Success(false); source.likeGate = CompletableDeferred()
        val holder = FeedDetailStateHolder(source, initial)
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.commitLike(holder.beginLike()!!) }
        source.full = Outcome.Success(initial.copy(
            counts = initial.counts.copy(
                liked = true,
                like = 20,
            ),
        )); holder.load()
        source.likeGate!!.complete(Unit); assertEquals("点赞失败", job.await())
        assertFalse(holder.state.value.liked); assertEquals(19, holder.state.value.likeCount)
    }

    @Test fun cancellationPropagatesAndKeepsOriginalOptimisticLike() = runBlocking {
        val source = Fake(); source.likeGate = CompletableDeferred()
        val holder = FeedDetailStateHolder(source, initial)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.commitLike(holder.beginLike()!!) }
        job.cancelAndJoin()
        assertTrue(job.isCancelled); assertTrue(holder.state.value.liked); assertEquals(3, holder.state.value.likeCount)
    }

    @Test fun closedPageRejectsLateDetailAndFutureActions() = runBlocking {
        val source = Fake(); source.full = Outcome.Success(initial.copy(text = "旧账号")); source.detailGate = CompletableDeferred()
        val holder = FeedDetailStateHolder(source, initial)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        holder.close(); source.detailGate!!.complete(Unit); job.join(); holder.load(); holder.loadIpLocation()
        assertEquals(FeedDetailUiState(initial), holder.state.value)
        assertNull(holder.beginLike()); assertNull(holder.commitLike(true))
        assertEquals(1, source.detailIds.size); assertTrue(source.ipMids.isEmpty()); assertTrue(source.likes.isEmpty())
    }

    @Test fun closedPageSuppressesLateIpAndLikeCompletion() = runBlocking {
        val source = Fake(); source.ip = Outcome.Success("广东"); source.liked = Outcome.Success(false)
        source.ipGate = CompletableDeferred(); source.likeGate = CompletableDeferred()
        val holder = FeedDetailStateHolder(source, initial)
        val ip = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadIpLocation() }
        val like = async(start = CoroutineStart.UNDISPATCHED) { holder.commitLike(holder.beginLike()!!) }
        holder.close(); val closed = holder.state.value
        source.ipGate!!.complete(Unit); source.likeGate!!.complete(Unit)
        ip.join(); assertNull(like.await()); assertEquals(closed, holder.state.value)
    }

    @Test fun olderFullDetailCannotReplaceNewerLoad() = runBlocking {
        val source = Fake(); source.full = Outcome.Success(initial.copy(text = "旧")); source.detailGate = CompletableDeferred()
        val holder = FeedDetailStateHolder(source, initial)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        val gate = source.detailGate!!; source.detailGate = null
        val latest = initial.copy(text = "新"); source.full = Outcome.Success(latest); holder.load()
        gate.complete(Unit); job.join(); assertEquals(latest, holder.state.value.detail)
    }
}
