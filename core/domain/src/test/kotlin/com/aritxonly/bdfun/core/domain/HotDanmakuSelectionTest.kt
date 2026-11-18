package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.DanmakuItem
import org.junit.Assert.assertEquals
import org.junit.Test

class HotDanmakuSelectionTest {
    private fun item(id: Long, text: String, likes: Long, time: Long = 20_000L) =
        DanmakuItem(time, 1, 0xFFFFFF, 25f, text, id, likes, count = 999)

    @Test fun reservesBeforeAppearanceAndReopensAfterTheLastEntryExpires() {
        val first = item(1, "first", 30, 20_000L)
        val second = item(2, "second", 40, 60_000L)
        val timeline = HotDanmakuTimeline(listOf(second, item(3, "low", 9, 19_000L), first))

        assertEquals(HotDanmakuWindow(), timeline.windowAt(9_999L, 10_000L))
        assertEquals(HotDanmakuWindow(upcoming = listOf(first)), timeline.windowAt(10_000L, 10_000L))
        assertEquals(HotDanmakuWindow(visible = listOf(first)), timeline.windowAt(20_000L, 10_000L))
        assertEquals(HotDanmakuWindow(visible = listOf(first)), timeline.windowAt(35_000L, 10_000L))
        assertEquals(HotDanmakuWindow(), timeline.windowAt(35_001L, 10_000L))
        assertEquals(HotDanmakuWindow(upcoming = listOf(second)), timeline.windowAt(51_000L, 10_000L))
        assertEquals(HotDanmakuWindow(upcoming = listOf(first)), timeline.windowAt(19_999L, 10_000L))
    }

    @Test fun reservationAllowsTheSlowestActiveTrackToFinishAtPlaybackSpeed() {
        assertEquals(9_500L, hotDanmakuReservationLeadMs(8_500L, 4_000L, 1f))
        assertEquals(13_000L, hotDanmakuReservationLeadMs(2_833L, 4_000L, 3f))
        assertEquals(9_500L, hotDanmakuReservationLeadMs(4_250L, 0L, 2f))
    }

    @Test fun overlongEntriesNeverForceClippingAndSingleEntryCanUseFullWidth() {
        assertEquals(listOf(1, 2), fittingHotDanmakuIndices(listOf(400f, 60f, 70f), 150f, 4f))
        assertEquals(listOf(0), fittingHotDanmakuIndices(listOf(130f), 150f, 4f))
        assertEquals(emptyList<Int>(), fittingHotDanmakuIndices(listOf(150f), 150f, 4f))
    }

    @Test fun interactiveSelectionKeepsOriginalIdentityAndLikeCount() {
        val original = item(99, "same text", 20)
        assertEquals(listOf(original), selectHotDanmakuItems(listOf(original), 20_000))
    }

    @Test fun timeWindowIncludesBothEndsAndRejectsOutside() {
        val input = listOf(item(1, "left", 12, 5_000), item(2, "right", 11),
            item(3, "old", 99, 4_999), item(4, "future", 99, 20_001))
        assertEquals(listOf("left" to 12L, "right" to 11L), selectHotDanmaku(input, 20_000))
    }
    @Test fun beginningOfPlaybackClampsWindowToZero() {
        assertEquals(listOf("start" to 10L), selectHotDanmaku(listOf(
            item(1, "negative", 99, -1), item(2, "start", 10, 0)), 100))
    }
    @Test fun invalidIdentityBlankTextAndLowLikesNeverQualify() {
        assertEquals(listOf("valid" to 10L), selectHotDanmaku(listOf(
            item(0, "no-id", 99), item(1, "  ", 99), item(2, "low", 9), item(3, "valid", 10)), 20_000))
    }
    @Test fun ranksBeforeDeduplicatingIdsAndTextAndLimitsToTwo() {
        val input = listOf(item(5, "repeat", 12), item(2, "duplicate-id", 39),
            item(2, "winner", 40), item(1, "repeat", 12), item(3, "third", 11))
        assertEquals(listOf("winner" to 40L, "repeat" to 12L), selectHotDanmaku(input, 20_000))
        assertEquals(listOf("top" to 13L, "lower-id" to 12L), selectHotDanmaku(listOf(
            item(5, "higher-id", 12), item(2, "lower-id", 12), item(3, "top", 13)), 20_000))
    }
}
