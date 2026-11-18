package com.aritxonly.bdfun.feature.library.application

import com.aritxonly.bdfun.core.model.library.HistoryItem
import org.junit.Assert.*
import org.junit.Test

class LibraryPresentationPoliciesTest {
    @Test fun entranceIsOncePerKeyAcrossScreensUntilBoundedEviction() {
        val registry=LibraryEntranceRegistry(2)
        assertTrue(registry.mark("0-BV1")); assertFalse(registry.mark("0-BV1"))
        assertTrue(registry.mark("1-BV1")); assertTrue(registry.mark("2-BV1"))
        assertTrue(registry.mark("0-BV1"))
    }
    @Test fun repeatedEntranceDoesNotChangeOldestInsertionOrder() {
        val registry=LibraryEntranceRegistry(2)
        registry.mark("A"); registry.mark("B"); assertFalse(registry.mark("A"))
        registry.mark("C"); assertFalse(registry.mark("B")); assertTrue(registry.mark("A"))
    }
    @Test(expected=IllegalArgumentException::class) fun entranceCapacityMustBePositive() { LibraryEntranceRegistry(0) }
    @Test fun searchDraftDoesNotCommitUntilSubmitAndSubmitReturnsUntrimmedText() {
        val h=LibrarySearchStateHolder(); h.changeDraft("  CAT  ")
        assertFalse(h.state.value.committed); assertEquals("  CAT  ",h.submit()); assertTrue(h.state.value.committed)
        h.changeDraft("dog"); assertFalse(h.state.value.submitted)
        h.clearInput(); assertEquals(LibrarySearchState(),h.state.value)
    }
    @Test fun historySelectionAndClearKeepOriginalSubmittedFlagRules() {
        val h=LibrarySearchStateHolder(); assertEquals("cat",h.chooseHistory("cat"))
        assertTrue(h.state.value.committed); h.clearHistoryDraft()
        assertTrue(h.state.value.submitted); assertFalse(h.state.value.committed)
        h.chooseHistory(" "); assertFalse(h.state.value.committed); assertNull(h.submit())
    }
    @Test fun historyProjectionRetainsOriginalQueueDefaultsAndCounts() {
        val history=HistoryItem(bvid="BV1",title="title",cover="cover",author="UP",viewedAt=456,progress=9,duration=100,play=42,danmaku=7,aid=123,cid=345)
        val video=history.toVideoItem()
        assertEquals("BV1",video.bvid); assertEquals(123L,video.aid); assertEquals("UP",video.ownerName)
        assertEquals("",video.ownerFace); assertEquals(0L,video.ownerMid)
        assertEquals(42L,video.play); assertEquals(7L,video.danmaku); assertEquals(100,video.duration); assertEquals(456L,video.pubdate)
        assertEquals(345L, video.cid)
    }
}
