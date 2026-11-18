package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.domain.activeSponsorSegmentAt
import com.aritxonly.bdfun.core.model.video.SponsorSegment
import org.junit.Assert.*
import org.junit.Test

class SponsorSegmentPolicyTest {
    private fun segment(id: String, start: Long, end: Long) = SponsorSegment(start, end, "sponsor", "skip", id)

    @Test fun startIsIncludedAndEndIsExcluded() {
        val segment = segment("one", 1000, 2000)
        assertNull(activeSponsorSegmentAt(listOf(segment), 999, emptySet()))
        assertEquals(segment, activeSponsorSegmentAt(listOf(segment), 1000, emptySet()))
        assertEquals(segment, activeSponsorSegmentAt(listOf(segment), 1999, emptySet()))
        assertNull(activeSponsorSegmentAt(listOf(segment), 2000, emptySet()))
    }

    @Test fun overlappingSegmentsKeepOriginalOrderAndDismissalRevealsNext() {
        val first = segment("first", 1000, 3000)
        val second = segment("second", 0, 4000)
        assertEquals(first, activeSponsorSegmentAt(listOf(first, second), 1500, emptySet()))
        assertEquals(second, activeSponsorSegmentAt(listOf(first, second), 1500, setOf("first")))
        assertNull(activeSponsorSegmentAt(listOf(first, second), 1500, setOf("first", "second")))
    }

    @Test fun zeroLengthReversedAndEmptySegmentsDoNotActivate() {
        assertNull(activeSponsorSegmentAt(listOf(segment("zero", 1000, 1000)), 1000, emptySet()))
        assertNull(activeSponsorSegmentAt(listOf(segment("backward", 2000, 1000)), 1500, emptySet()))
        assertNull(activeSponsorSegmentAt(emptyList(), 0, emptySet()))
    }

    @Test fun skipModeMapsToOffPromptOrAuto() {
        assertEquals(SponsorSkipAction.Off, sponsorSkipAction(0))
        assertEquals(SponsorSkipAction.Prompt, sponsorSkipAction(1))
        assertEquals(SponsorSkipAction.Auto, sponsorSkipAction(2))
        // 未知值沿用旧默认的「提示」，不静默关闭或自动跳转。
        assertEquals(SponsorSkipAction.Prompt, sponsorSkipAction(99))
    }
}
