package com.aritxonly.bdfun.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuSegmentsTest {
    @Test fun positionMapsToSixMinuteSegments() {
        assertEquals(1, danmakuSegmentIndex(0))
        assertEquals(1, danmakuSegmentIndex(359_999))
        assertEquals(2, danmakuSegmentIndex(360_000))
        assertEquals(3, danmakuSegmentIndex(720_000))
        assertEquals(1, danmakuSegmentIndex(-5))
    }

    @Test fun segmentCountRoundsUpAndNeverZero() {
        assertEquals(1, danmakuSegmentCount(0))
        assertEquals(1, danmakuSegmentCount(360))
        assertEquals(2, danmakuSegmentCount(361))
        assertEquals(20, danmakuSegmentCount(7200))
    }

    @Test fun windowIsCurrentPlusNeighboursClampedToVideo() {
        assertEquals(listOf(1, 2), danmakuSegmentWindow(0, 20))
        assertEquals(listOf(1, 2, 3), danmakuSegmentWindow(400_000, 20))
        assertEquals(listOf(4, 5, 6), danmakuSegmentWindow(4 * 360_000L, 20))
        assertEquals(listOf(19, 20), danmakuSegmentWindow(20 * 360_000L, 20))
    }

    @Test fun unknownTotalStillReturnsValidWindow() {
        assertEquals(listOf(1, 2), danmakuSegmentWindow(0))
        assertEquals(listOf(10, 11, 12), danmakuSegmentWindow(10 * 360_000L))
    }
}
