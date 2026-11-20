package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class VideoOrientationPolicyTest {
    @Test fun compactPhoneLandscapeEntersFullscreen() {
        assertEquals(true, fullscreenAfterVideoRotation(true, 393, true))
        assertEquals(true, fullscreenAfterVideoRotation(true, 599, true))
    }
    @Test fun expandedFoldableKeepsLandscapePageAndManualFullscreen() {
        assertNull(fullscreenAfterVideoRotation(true, 600, true))
        assertNull(fullscreenAfterVideoRotation(true, 840, true))
    }
    @Test fun disabledRotationAndUnknownSizeDoNotForceFullscreen() {
        assertNull(fullscreenAfterVideoRotation(true, 393, false))
        assertNull(fullscreenAfterVideoRotation(true, 0, true))
    }
    @Test fun portraitLeavesFullscreenOnEitherDevice() {
        assertEquals(false, fullscreenAfterVideoRotation(false, 393, true))
        assertEquals(false, fullscreenAfterVideoRotation(false, 840, false))
    }

    @Test fun videoAspectRatioHandlesValidAndMissingDimensions() {
        assertEquals(16f / 9f, videoAspectRatio(1920, 1080)!!, 0.0001f)
        assertEquals(9f / 16f, videoAspectRatio(1080, 1920)!!, 0.0001f)
        assertNull(videoAspectRatio(0, 1080))
        assertNull(videoAspectRatio(1920, -1))
    }

    @Test fun verticalAspectDetectionTreatsUnknownAsLandscape() {
        assertTrue(isVerticalVideoAspect(9f / 16f))
        assertTrue(isVerticalVideoAspect(0.75f))
        assertFalse(isVerticalVideoAspect(16f / 9f))
        assertFalse(isVerticalVideoAspect(1f))
        assertFalse(isVerticalVideoAspect(null))
        assertFalse(isVerticalVideoAspect(0f))
        assertFalse(isVerticalVideoAspect(-0.5f))
    }

    @Test fun pipAspectPairsApproximateCommonRatios() {
        assertEquals(16 to 9, pipAspectPair(16f / 9f))
        assertEquals(9 to 16, pipAspectPair(9f / 16f))
        assertEquals(Pair(4, 3), pipAspectPair(4f / 3f))
        assertEquals(16 to 9, pipAspectPair(null))
        assertEquals(16 to 9, pipAspectPair(0f))
        assertEquals(16 to 9, pipAspectPair(Float.NaN))
        // 系统要求分子分母都在 1..100，极端比例也要落在范围内
        val extreme = pipAspectPair(0.01f)
        assertTrue(extreme.first in 1..100 && extreme.second in 1..100)
    }
}
