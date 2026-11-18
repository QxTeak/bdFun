package com.aritxonly.bdfun.feature.video.application

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class DiscGesturePolicyTest {
    private fun action(x: Float, y: Float, long: Boolean = false) =
        discReleaseAction(x, y, sqrt(x * x + y * y), long, 32f, 8f)
    @Test fun longPressReleaseDoesNothingEvenAfterLargeMovement() {
        for ((x, y) in listOf(0f to 0f, -40f to 0f, 0f to -40f, 40f to 40f))
            assertEquals(DiscReleaseAction.None, action(x, y, true))
    }
    @Test fun horizontalThresholdIsInclusiveAndWinsTiesInBothDirections() {
        assertEquals(DiscReleaseAction.Next, action(-32f, 0f))
        assertEquals(DiscReleaseAction.Previous, action(32f, 0f))
        assertEquals(DiscReleaseAction.Next, action(-32f, -32f))
        assertEquals(DiscReleaseAction.Previous, action(32f, 32f))
        assertEquals(DiscReleaseAction.None, action(31.999f, 0f))
    }
    @Test fun verticalActionsRequireStrictlyLargerVerticalMovement() {
        assertEquals(DiscReleaseAction.Exit, action(0f, -32f))
        assertEquals(DiscReleaseAction.TogglePlayback, action(0f, 32f))
        assertEquals(DiscReleaseAction.Exit, action(32f, -32.001f))
        assertEquals(DiscReleaseAction.TogglePlayback, action(-32f, 32.001f))
        assertEquals(DiscReleaseAction.None, action(0f, -31.999f))
    }
    @Test fun tapsRequireStrictDistanceBelowTouchSlop() {
        assertEquals(DiscReleaseAction.Open, action(0f, 0f))
        assertEquals(DiscReleaseAction.Open, action(7.999f, 0f))
        assertEquals(DiscReleaseAction.None, action(8f, 0f))
        assertEquals(DiscReleaseAction.None, action(6f, 6f))
        assertEquals(DiscReleaseAction.None, action(31f, 31f))
    }
    @Test fun dragDoesNotMoveDiscUntilInclusiveArmingThreshold() {
        assertEquals(DiscDragOffset(0f, 0f, false), discDragOffset(31.99f, -31.99f, false, 32f, 8f))
        assertEquals(DiscDragOffset(8f, 0f, true), discDragOffset(32f, 0f, false, 32f, 8f))
        assertEquals(DiscDragOffset(0f, -8f, true), discDragOffset(0f, -32f, false, 32f, 8f))
    }
    @Test fun armedDragKeepsFollowingOnReturnAndClampsOnlyDominantAxis() {
        assertEquals(DiscDragOffset(2f, 0f, true), discDragOffset(2f, 1f, true, 32f, 8f))
        assertEquals(DiscDragOffset(0f, -3f, true), discDragOffset(1f, -3f, true, 32f, 8f))
        assertEquals(DiscDragOffset(-8f, 0f, true), discDragOffset(-50f, -50f, true, 32f, 8f))
        assertEquals(DiscDragOffset(0f, 0f, true), discDragOffset(0f, 0f, true, 32f, 8f))
    }
}
