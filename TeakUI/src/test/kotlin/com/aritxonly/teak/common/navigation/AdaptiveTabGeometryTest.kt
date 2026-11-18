package com.aritxonly.teak.common.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTabGeometryTest {
    @Test fun truncatedEdgeHintsMoveInwardWithoutMovingCenteredLabels() {
        val g = AdaptiveTabGeometry(5, 180f, adaptiveTabItemWidth(180f, 3))
        assertTrue(g.edgeInset(0, 2f) > 0f)
        assertTrue(g.edgeInset(4, 2f) < 0f)
        assertEquals(0f, g.edgeInset(2, 2f), .001f)
        assertEquals(0f, g.edgeInset(0, 0f), .001f)
        assertEquals(0f, g.edgeInset(4, 4f), .001f)
        assertTrue(kotlin.math.abs(g.edgeInset(4, 2f)) <= g.item * .40f)
    }

    @Test fun threeForegroundTabsOccupyAtLeastNinetyPercentAtEveryStop() {
        for (count in listOf(5, 7)) {
            val g = AdaptiveTabGeometry(count, 180f, adaptiveTabItemWidth(180f, 3, 4f))
            for (selected in 0 until count) {
                val first = (selected - 1).coerceIn(0, count - 3)
                val left = first * g.item - g.scroll(selected.toFloat())
                assertTrue(left >= -.001f)
                assertTrue(left + 3 * g.item <= g.viewport + .001f)
                assertTrue(3 * g.item >= .90f * (g.viewport + 8f))
                for (index in first..first + 2) {
                    assertEquals(1f, g.labelScale(index, selected.toFloat()), .001f)
                }
            }
        }
    }

    @Test fun peripheralLabelsShrinkAndMoveTowardTheMainTabsEvenAtEndpoints() {
        val g = AdaptiveTabGeometry(5, 180f, adaptiveTabItemWidth(180f, 3))
        assertTrue(g.edgeInset(3, 0f) < 0f)
        assertTrue(g.edgeInset(1, 4f) > 0f)
        assertEquals(.82f, g.labelScale(3, 0f), .001f)
        assertEquals(.82f, g.labelScale(4, 2f), .001f)
        assertEquals(-g.edgeInset(3, 0f), g.edgeInset(1, 4f), .001f)
        assertEquals(-g.edgeInset(4, 2f), g.edgeInset(0, 2f), .001f)
    }

    @Test fun limitsRoundDownAndStayPositive() {
        assertEquals(3, normalizedTabLimit(4))
        assertEquals(3, normalizedTabLimit(3))
        assertEquals(1, normalizedTabLimit(0))
        assertEquals(1, normalizedTabLimit(Int.MIN_VALUE))
    }

    @Test fun endpointsHaveNoBlankSpace() {
        val g = AdaptiveTabGeometry(7, 180f, adaptiveTabItemWidth(180f, 3))
        assertEquals(0f, g.scroll(0f), .001f)
        assertEquals(0f, g.indicator(0f), .001f)
        assertEquals(g.maxScroll, g.scroll(6f), .001f)
        assertEquals(g.viewport, g.indicator(6f) + g.item, .001f)
    }

    @Test fun middleSelectionKeepsIndicatorStationaryIncludingPageSwipes() {
        val g = AdaptiveTabGeometry(7, 180f, adaptiveTabItemWidth(180f, 3))
        for (step in 20..40) {
            assertEquals((g.viewport - g.item) / 2f, g.indicator(step / 10f), .001f)
        }
        assertTrue(g.scroll(4f) > g.scroll(2f))
    }

    @Test fun singleSlotAndNarrowWidthsStayBounded() {
        for (width in listOf(32f, 74f, 180f)) {
            val g = AdaptiveTabGeometry(7, width, adaptiveTabItemWidth(width, 1))
            for (i in 0..6) {
                assertTrue(g.scroll(i.toFloat()) in 0f..g.maxScroll)
                assertTrue(g.indicator(i.toFloat()) >= -.001f)
                assertTrue(g.indicator(i.toFloat()) + g.item <= width + .001f)
            }
        }
    }
}
