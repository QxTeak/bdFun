package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.model.video.VideoDetailOwner

import com.aritxonly.bdfun.core.model.video.VideoDetailInfo

import com.aritxonly.bdfun.core.model.video.VideoDetailCounts

import com.aritxonly.bdfun.core.model.video.*
import org.junit.Assert.*
import org.junit.Test

class VideoReadCacheTest {
    private var owner = VideoReadCacheOwner(1, 0)
    private val cache = VideoReadCache { owner }
    private fun video(id: String) = VideoItem(id, 42, id, "", "UP", "", 7, 0, 0, 0, 0)
    private fun entry(id: String) = VideoCacheEntry(
        VideoDetail(
            bvid = id,
            aid = 42,
            title = id,
            cover = "",
            desc = "",
            owner = VideoDetailOwner(
                ownerName = "UP",
                ownerFace = "",
                ownerMid = 7,
                ownerSign = "",
            ),
            counts = VideoDetailCounts(
                play = 0,
                danmaku = 0,
                like = 0,
                coin = 0,
                favorite = 0,
                share = 0,
                reply = 0,
            ),
            info = VideoDetailInfo(
                pubdate = 0,
                duration = 10,
                cid = 11,
                tname = "",
                pages = emptyList(),
            ),
        ), null, emptyList(), emptyList())

    @Test fun cachedStreamMustMatchTheRequestedPart() {
        val defaultEntry = entry("one")
        val detail = defaultEntry.detail
        val laterCid = detail.cid + 1
        val multiPart = detail.copy(info = detail.info.copy(
            pages = listOf(VideoPage(laterCid, 2, "第二段", 20)),
        ))
        val defaultPart = defaultEntry.copy(detail = multiPart)
        val laterPart = defaultPart.copy(playbackCid = laterCid)
        assertFalse(defaultPart.matchesPart(laterCid))
        assertTrue(laterPart.matchesPart(laterCid))
        assertFalse(laterPart.matchesPart(0L))
        assertTrue(defaultPart.matchesPart(laterCid + 1))
    }

    @Test fun detailAndRelatedAreSharedBySameAccountPages() {
        val first = cache.scope(); val next = cache.scope()
        first.putRelated("a", listOf(video("related"))); first.putVideo("a", entry("a"))
        assertEquals("related", next.related("a")!!.single().bvid)
        assertEquals("a", next.video("a")!!.detail.bvid)
    }
    @Test fun emptyRelatedCannotEraseExistingRows() {
        val page = cache.scope(); page.putRelated("a", listOf(video("old"))); page.putRelated("a", emptyList())
        assertEquals(listOf(video("old")), page.related("a"))
        page.putRelated("missing", emptyList()); assertNull(page.related("missing"))
    }
    @Test fun detailEvictsInsertionOrderAndReadDoesNotMakeItLru() {
        val page = cache.scope()
        for (i in 1..6) page.putVideo("$i", entry("$i"))
        assertNotNull(page.video("1")); page.putVideo("7", entry("7"))
        assertNull(page.video("1")); assertNotNull(page.video("2"))
    }
    @Test fun replacingAndUpdatingAnEntryDoNotRefreshItsEvictionOrder() {
        val page = cache.scope()
        for (i in 1..6) page.putVideo("$i", entry("$i"))
        page.putVideo("1", entry("replacement")); page.updateVideo("1") { it.copy(interaction = it.interaction.copy(liked = true)) }
        page.putVideo("7", entry("7")); assertNull(page.video("1"))
    }
    @Test fun relatedUsesOriginalHashMapEvictionAtTwelve() {
        val page = cache.scope(); val original = HashMap<String, List<VideoItem>>()
        for (i in 1..30) {
            val key = "BV$i"; val rows = listOf(video(key))
            original[key] = rows
            while (original.size > 12) original.remove(original.keys.first())
            page.putRelated(key, rows)
            for (n in 1..i) assertEquals(original["BV$n"], page.related("BV$n"))
        }
    }
    @Test fun interactionUpdateKeepsOtherEntryFieldsAndMissingDoesNotInsert() {
        val page = cache.scope(); val original = entry("a").copy(related = listOf(video("r")))
        page.putVideo("a", original)
        page.updateVideo("a") { it.copy(interaction = it.interaction.copy(liked = true, coined = true, favorited = true), counts = it.counts.copy(like = 12, coin = 2, favorite = 3)) }
        val updated = page.video("a")!!
        assertEquals(original.copy(
            detail = original.detail.copy(
                interaction = original.detail.interaction.copy(
                    liked = true,
                    coined = true,
                    favorited = true,
                ),
                counts = original.detail.counts.copy(
                    like = 12,
                    coin = 2,
                    favorite = 3,
                ),
            ),
        ), updated)
        page.updateVideo("missing") { error("must not transform") }; assertNull(page.video("missing"))
    }
    @Test fun rotationClosesOldPageButKeepsRowsForReplacement() {
        val old = cache.scope(); old.putRelated("a", listOf(video("r"))); old.putVideo("a", entry("a")); old.close()
        val next = cache.scope(); assertNotNull(next.video("a")); assertNotNull(next.related("a"))
        assertNull(old.related("a")); old.putRelated("late", listOf(video("late")))
        old.putVideo("late", entry("late")); assertNull(next.video("late")); assertNull(next.related("late"))
        next.putVideo("a", entry("replacement"))
        old.updateVideo("a") { error("closed page must not transform") }; old.close("a")
        assertEquals("replacement", next.video("a")!!.detail.bvid); assertNotNull(next.related("a"))
    }
    @Test fun realExitOnlyClearsCurrentRelatedAndPreservesDetail() {
        val old = cache.scope(); old.putVideo("a", entry("a"))
        old.putRelated("a", listOf(video("a"))); old.putRelated("b", listOf(video("b")))
        old.close("a"); val next = cache.scope()
        assertNull(next.related("a")); assertNotNull(next.related("b")); assertNotNull(next.video("a"))
    }
    @Test fun blankExitDoesNotClearRelatedAndCloseIsIdempotent() {
        val old = cache.scope(); old.putRelated("a", listOf(video("a"))); old.close(" "); old.close("a")
        assertNotNull(cache.scope().related("a"))
    }
    @Test fun accountSwitchClearsBothCachesAndRejectsAllLateOperations() {
        val old = cache.scope(); old.putRelated("a", listOf(video("old"))); old.putVideo("a", entry("old"))
        owner = VideoReadCacheOwner(2, 1)
        val next = cache.scope(); assertNull(next.video("a")); assertNull(next.related("a"))
        next.putVideo("a", entry("new")); next.putRelated("a", listOf(video("new")))
        old.putVideo("a", entry("stale")); old.putRelated("a", listOf(video("stale")))
        old.updateVideo("a") { error("old account must not transform") }; old.close("a")
        assertEquals("new", next.video("a")!!.detail.bvid); assertEquals("new", next.related("a")!!.single().bvid)
    }
    @Test fun aToBToAWithoutIntermediateCacheAccessDoesNotReviveOldPage() {
        val old = cache.scope(); old.putVideo("a", entry("old"))
        owner = VideoReadCacheOwner(2, 1); owner = VideoReadCacheOwner(1, 2)
        assertNull(old.video("a")); old.putVideo("a", entry("stale"))
        assertNull(cache.scope().video("a"))
    }
    @Test fun guestLoginLogoutAndSameMidNewRevisionInvalidateOldHandles() {
        val old = cache.scope(); old.putVideo("a", entry("old"))
        owner = VideoReadCacheOwner(1, 1); assertNull(old.video("a"))
        val signedIn = cache.scope(); signedIn.putVideo("a", entry("signed"))
        owner = VideoReadCacheOwner(null, 2); assertNull(signedIn.video("a"))
        val guest = cache.scope(); guest.putVideo("a", entry("guest"))
        owner = VideoReadCacheOwner(1, 3); assertNull(guest.video("a")); assertNull(cache.scope().video("a"))
    }
    @Test fun separateContainersDoNotShareStorage() {
        cache.scope().putVideo("a", entry("a"))
        assertNull(VideoReadCache { owner }.scope().video("a"))
    }

    @Test fun closingContainerRejectsLateReadsWritesAndNewPageScopes() {
        val page = cache.scope(); page.putVideo("a", entry("a")); page.putRelated("a", listOf(video("r")))
        cache.close(); cache.close()
        assertNull(page.video("a")); assertNull(page.related("a"))
        page.putVideo("a", entry("late")); page.putRelated("a", listOf(video("late")))
        page.updateVideo("a") { error("closed container must not transform") }; page.close("a")
        try { cache.scope(); fail("closed container cannot create pages") }
        catch (_: IllegalStateException) { }
    }
}
