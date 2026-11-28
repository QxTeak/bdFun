package com.aritxonly.bdfun.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedUpExitPullTest {
    @Test
    fun pullProgressFollowsDragDistance() {
        assertEquals(1f, feedUpExitProgress(0f, 500f))
        assertEquals(0.5f, feedUpExitProgress(250f, 500f))
        assertEquals(0f, feedUpExitProgress(500f, 500f))
        // 拉过头或往回拉都不能越界。
        assertEquals(0f, feedUpExitProgress(900f, 500f))
        assertEquals(1f, feedUpExitProgress(-80f, 500f))
    }

    @Test
    fun shortSlowPullSpringsBack() {
        val progress = feedUpExitProgress(80f, 500f)
        assertFalse(shouldCompleteFeedUpExit(progress, velocityPxPerSec = 200f, flingVelocityPxPerSec = 3000f))
    }

    @Test
    fun farPullCompletesEvenWithoutFling() {
        val progress = feedUpExitProgress(400f, 500f)
        assertTrue(shouldCompleteFeedUpExit(progress, velocityPxPerSec = 0f, flingVelocityPxPerSec = 3000f))
    }

    @Test
    fun fastFlingCompletesFromSmallDistance() {
        val progress = feedUpExitProgress(20f, 500f)
        assertTrue(shouldCompleteFeedUpExit(progress, velocityPxPerSec = 3200f, flingVelocityPxPerSec = 3000f))
    }
}
