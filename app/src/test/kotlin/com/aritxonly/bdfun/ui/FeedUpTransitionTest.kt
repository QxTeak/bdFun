package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 头像环「从哪一侧滑进来」的纯几何规则。 */
class FeedRingGeometryTest {
    private val width = 400f

    private fun avatar(left: Float, size: Float = 34f) = Rect(left, 0f, left + size, size)

    @Test fun partiallyVisibleAvatarRemembersTheSideItPokesOut() {
        assertEquals(FeedRingSide.Left, feedRingExitSide(avatar(-20f), width))
        assertEquals(FeedRingSide.Right, feedRingExitSide(avatar(386f), width))
        assertEquals(FeedRingSide.Left, feedRingExitSide(avatar(-40f), width))
        assertEquals(FeedRingSide.Right, feedRingExitSide(avatar(420f), width))
    }

    @Test fun fullyVisibleAvatarHasNoExitSide() {
        assertNull(feedRingExitSide(avatar(120f), width))
    }

    @Test fun offscreenStartIsFullyOutsideAndAlignedToTheTargetRow() {
        val left = feedRingOffscreenStart(FeedRingSide.Left, avatarSize = 34f, centerY = 100f, viewportWidth = width)
        assertTrue("left start must stay outside: ${left.right}", left.right <= 0f)
        assertEquals(34f, left.width, 0.001f)
        assertEquals(100f, left.top + left.height / 2f, 0.001f)
        assertTrue(feedRingIsOffscreen(left, width))

        val right = feedRingOffscreenStart(FeedRingSide.Right, avatarSize = 34f, centerY = 100f, viewportWidth = width)
        assertTrue("right start must stay outside: ${right.left}", right.left >= width)
        assertEquals(100f, right.top + right.height / 2f, 0.001f)
        assertTrue(feedRingIsOffscreen(right, width))
    }

    @Test fun nearestSideFallbackMatchesTheCloserEdge() {
        assertEquals(FeedRingSide.Left, feedRingNearestSide(avatar(10f), width))
        assertEquals(FeedRingSide.Right, feedRingNearestSide(avatar(360f), width))
    }

    @Test fun upSwitchDirectionFollowsAvatarOrder() {
        val order = listOf(9L, 2L, 3L)
        assertEquals(1, feedUpSwitchDirection(2L, 3L, order))
        assertEquals(-1, feedUpSwitchDirection(3L, 2L, order))
        assertEquals(1, feedUpSwitchDirection(9L, 3L, order))
        assertEquals(0, feedUpSwitchDirection(2L, 2L, order))
    }

    @Test fun enteringOrLeavingAllModeOnlyFades() {
        val order = listOf(9L, 2L)
        assertEquals(0, feedUpSwitchDirection(null, 2L, order))
        assertEquals(0, feedUpSwitchDirection(2L, null, order))
        // UP 列表还没加载到目标时也退化为淡入，不能瞎猜方向。
        assertEquals(0, feedUpSwitchDirection(7L, 2L, order))
    }

    @Test fun pushOffsetsContinueFromDragAndLandOnTheCorrectSides() {
        val width = 1000f
        val tapNext = feedUpPushOffsets(dragOffset = 0f, direction = 1, width = width)
        assertEquals(-width, tapNext.outgoingEnd, 0.001f)
        assertEquals(width, tapNext.contentStart, 0.001f)

        val draggedNext = feedUpPushOffsets(dragOffset = -120f, direction = 1, width = width)
        assertEquals(-width, draggedNext.outgoingEnd, 0.001f)
        assertEquals(width - 120f, draggedNext.contentStart, 0.001f)
        // 松手后两页位移相同：旧页从拖动位置滑出左侧，新页正好落到 0。
        assertEquals(-120f + draggedNext.shift, -width, 0.001f)
        assertEquals(width - 120f + draggedNext.shift, 0f, 0.001f)

        val draggedPrevious = feedUpPushOffsets(dragOffset = 140f, direction = -1, width = width)
        assertEquals(width, draggedPrevious.outgoingEnd, 0.001f)
        assertEquals(140f - width, draggedPrevious.contentStart, 0.001f)
        assertEquals(140f + draggedPrevious.shift, width, 0.001f)
        assertEquals(140f - width + draggedPrevious.shift, 0f, 0.001f)
    }
}
