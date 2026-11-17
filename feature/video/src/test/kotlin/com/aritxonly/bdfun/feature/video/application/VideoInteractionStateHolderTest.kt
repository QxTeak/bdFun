package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.model.video.VideoDetailOwner

import com.aritxonly.bdfun.core.model.video.VideoDetailInfo

import com.aritxonly.bdfun.core.model.video.VideoDetailCounts

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.VideoActionsRepository
import com.aritxonly.bdfun.core.model.library.FavoriteFolder
import com.aritxonly.bdfun.core.model.video.VideoDetail
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class VideoInteractionStateHolderTest {
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
            like = 10,
            coin = 3,
            favorite = 4,
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
    private class Fake : VideoActionsRepository {
        var likeAction: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
        var favoriteAction: suspend () -> Outcome<Boolean> = { Outcome.Success(true) }
        var foldersAction: suspend () -> Outcome<List<FavoriteFolder>> = { Outcome.Success(listOf(FavoriteFolder(1, "默认", 0, ""))) }
        var coinResult: Outcome<Unit> = Outcome.Success(Unit)
        var calls = 0
        var removals = mutableListOf<Long>()
        override suspend fun like(aid: Long, enabled: Boolean): Outcome<Unit> { calls++; return likeAction() }
        override suspend fun dislike(aid: Long, enabled: Boolean) = likeAction()
        override suspend fun coin(aid: Long, count: Int, alsoLike: Boolean) = coinResult
        override suspend fun follow(mid: Long, enabled: Boolean) = likeAction()
        override suspend fun watchLater(aid: Long): Outcome<Boolean> = Outcome.Success(false)
        override suspend fun folders() = foldersAction()
        override suspend fun folderIds(aid: Long): Outcome<Set<Long>> = Outcome.Success(setOf(1))
        override suspend fun setFavorite(aid: Long, folderId: Long, enabled: Boolean): Outcome<Boolean> {
            removals += folderId
            return favoriteAction()
        }
        override suspend fun removeFavorites(aid: Long, folderIds: Set<Long>): Outcome<Unit> = Outcome.Failure(AppError.Network("offline"))
    }
    private fun holder(fake: Fake = Fake()) = VideoInteractionStateHolder(fake).apply { bind(detail) }

    @Test fun failedLikeRollsBackCountAndHighlight() = runBlocking {
        val fake = Fake().apply { likeAction = { Outcome.Failure(AppError.Network("offline")) } }
        val holder = holder(fake)
        assertEquals("offline", holder.toggleLike())
        assertFalse(holder.state.value.liked)
        assertEquals(10L, holder.state.value.likeCount)
        assertFalse(holder.state.value.busy)
    }

    @Test fun cancelledOptimisticWriteRollsBackAndUnlocks() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { likeAction = { gate.await(); Outcome.Success(Unit) } }
        val holder = holder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.toggleLike() }
        assertTrue(holder.state.value.liked)
        job.cancelAndJoin()
        assertFalse(holder.state.value.liked)
        assertFalse(holder.state.value.busy)
    }

    @Test fun repeatedLikeDoesNotSubmitTwice() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { likeAction = { gate.await(); Outcome.Success(Unit) } }
        val holder = holder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.toggleLike() }
        assertNull(holder.toggleLike())
        assertEquals(1, fake.calls)
        gate.complete(Unit); job.join()
        assertEquals(11L, holder.state.value.likeCount)
    }

    @Test fun oldFailureCannotRollbackNewEpisode() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val fake = Fake().apply { likeAction = { gate.await(); Outcome.Failure(AppError.Network("offline")) } }
        val holder = holder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.toggleLike() }
        holder.bind(detail.copy(
            bvid = "two",
            aid = 43,
            interaction = detail.interaction.copy(
                liked = true,
            ),
            counts = detail.counts.copy(
                like = 100,
            ),
        ))
        gate.complete(Unit); job.join()
        assertTrue(holder.state.value.liked)
        assertEquals(100L, holder.state.value.likeCount)
        assertFalse(holder.state.value.busy)
    }

    @Test fun failedDislikeRestoresExistingLike() = runBlocking {
        val fake = Fake().apply { likeAction = { Outcome.Failure(AppError.Network("offline")) } }
        val holder = VideoInteractionStateHolder(fake).apply { bind(detail.copy(
            interaction = detail.interaction.copy(
                liked = true,
            ),
        )) }
        holder.toggleDislike()
        assertTrue(holder.state.value.liked)
        assertFalse(holder.state.value.disliked)
        assertEquals(10L, holder.state.value.likeCount)
    }

    @Test fun relationResponseCannotOverwriteRecentFollow() = runBlocking {
        val holder = holder()
        val old = holder.relationToken()
        holder.toggleFollow(7)
        holder.restoreFollowed(false, old)
        assertTrue(holder.state.value.followed)
    }

    @Test fun rejectedFolderWriteKeepsSelectionAndReleasesPending() = runBlocking {
        val fake = Fake().apply { favoriteAction = { Outcome.Success(false) } }
        val holder = holder(fake)
        holder.loadFavorites()
        assertEquals("取消收藏失败", holder.toggleFolder(1))
        assertEquals(setOf(1L), holder.state.value.folderIds)
        assertNull(holder.state.value.pendingFolder)
        assertFalse(holder.state.value.busy)
    }

    @Test fun cancelledFolderPrefetchCanRetry() = runBlocking {
        val fake = Fake().apply { foldersAction = { awaitCancellation() } }
        val holder = holder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadFavorites() }
        job.cancelAndJoin()
        assertFalse(holder.state.value.favoritesLoading)
        assertFalse(holder.state.value.favoritesLoaded)
        fake.foldersAction = { Outcome.Success(emptyList()) }
        holder.loadFavorites()
        assertTrue(holder.state.value.favoritesLoaded)
    }

    @Test fun tripleKeepsSuccessfulPartsWhenCoinFails() = runBlocking {
        val fake = Fake().apply { coinResult = Outcome.Failure(AppError.Network("offline")) }
        val holder = holder(fake)
        assertEquals("一键三连部分失败", holder.tripleAction())
        assertTrue(holder.state.value.liked)
        assertFalse(holder.state.value.coined)
        assertTrue(holder.state.value.favorited)
        assertEquals(11L, holder.state.value.likeCount)
        assertEquals(3L, holder.state.value.coinCount)
    }

    @Test fun declinedWatchLaterDoesNotReportSuccess() = runBlocking {
        assertEquals("添加稍后再看失败", holder().watchLater())
    }

    @Test fun coinWithAlsoLikeMarksLikedAndBumpsCountOnce() = runBlocking {
        val fake = Fake()
        val holder = holder(fake)
        assertEquals("投币成功", holder.coin(count = 2, alsoLike = true))
        assertTrue(holder.state.value.coined)
        assertTrue(holder.state.value.liked)
        assertEquals(5L, holder.state.value.coinCount)
        assertEquals(11L, holder.state.value.likeCount)
        // 已经点赞过时再投币不应重复加赞。
        holder.coin(count = 1, alsoLike = true)
        assertEquals(11L, holder.state.value.likeCount)
    }
}
