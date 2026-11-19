package com.aritxonly.bdfun.presentation.popular

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.PopularRepository
import com.aritxonly.bdfun.core.model.video.PopularPage
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PopularPageStateHolderTest {
    private fun video(id: Long) =
        VideoItem("BV$id", id, "标题$id", "", "UP", "", 1, id, 0, 1, 0)

    private class Fake : PopularRepository {
        val pages = mutableMapOf<Int, PopularPage>()
        var failNext = false
        val requested = mutableListOf<Int>()
        override suspend fun page(page: Int): Outcome<PopularPage> {
            requested += page
            if (failNext) {
                failNext = false
                return Outcome.Failure(AppError.Network("offline"))
            }
            return Outcome.Success(pages[page] ?: PopularPage(emptyList(), hasMore = false))
        }
    }

    @Test fun firstPageThenAppendDeduplicatesByBvid() = runBlocking {
        val source = Fake()
        source.pages[1] = PopularPage(listOf(video(1), video(2)), hasMore = true)
        source.pages[2] = PopularPage(listOf(video(2), video(3)), hasMore = true)
        val holder = PopularPageStateHolder(source)

        holder.ensureLoaded()
        assertEquals(listOf("BV1", "BV2"), holder.videos.map { it.bvid })
        assertFalse(holder.end)

        holder.loadMore()
        assertEquals(listOf("BV1", "BV2", "BV3"), holder.videos.map { it.bvid })
        assertEquals(listOf(1, 2), source.requested)
    }

    @Test fun noMoreStopsFurtherPaging() = runBlocking {
        val source = Fake()
        source.pages[1] = PopularPage(listOf(video(1)), hasMore = false)
        val holder = PopularPageStateHolder(source)
        holder.ensureLoaded()
        assertTrue(holder.end)

        holder.loadMore()
        assertEquals(listOf(1), source.requested)
    }

    @Test fun failureKeepsExistingListAndRetryRecovers() = runBlocking {
        val source = Fake()
        source.pages[1] = PopularPage(listOf(video(1)), hasMore = true)
        val holder = PopularPageStateHolder(source)
        holder.ensureLoaded()

        source.failNext = true
        holder.loadMore()
        assertEquals(1, holder.videos.size)
        assertEquals("offline", holder.error)

        holder.retry()
        assertNull(holder.error)
        assertEquals(listOf("BV1"), holder.videos.map { it.bvid })
    }

    @Test fun emptyFirstPageIsNotAnError() = runBlocking {
        val source = Fake()
        val holder = PopularPageStateHolder(source)
        holder.ensureLoaded()
        assertTrue(holder.videos.isEmpty())
        assertTrue(holder.loadedOnce)
        assertTrue(holder.end)
        assertNull(holder.error)
    }
}
