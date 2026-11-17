package com.aritxonly.teak.common.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTabGeometryTest {
    @Test fun limitsRoundDownAndStayPositive() {
        assertEquals(3, normalizedTabLimit(4))
        assertEquals(3, normalizedTabLimit(3))
        assertEquals(1, normalizedTabLimit(0))
        assertEquals(1, normalizedTabLimit(Int.MIN_VALUE))
    }

    @Test fun endpointsHaveNoBlankSpace() {
        val g = AdaptiveTabGeometry(7, 180f, 180f / 3.6f)
        assertEquals(0f, g.scroll(0f), .001f)
        assertEquals(0f, g.indicator(0f), .001f)
        assertEquals(g.maxScroll, g.scroll(6f), .001f)
        assertEquals(g.viewport, g.indicator(6f) + g.item, .001f)
    }

    @Test fun middleSelectionKeepsIndicatorStationaryIncludingPageSwipes() {
        val g = AdaptiveTabGeometry(7, 180f, 180f / 3.6f)
        for (step in 20..40) {
            assertEquals((g.viewport - g.item) / 2f, g.indicator(step / 10f), .001f)
        }
        assertTrue(g.scroll(4f) > g.scroll(2f))
    }

    @Test fun singleSlotAndNarrowWidthsStayBounded() {
        for (width in listOf(32f, 74f, 180f)) {
            val g = AdaptiveTabGeometry(7, width, width / 1.6f)
            for (i in 0..6) {
                assertTrue(g.scroll(i.toFloat()) in 0f..g.maxScroll)
                assertTrue(g.indicator(i.toFloat()) >= -.001f)
                assertTrue(g.indicator(i.toFloat()) + g.item <= width + .001f)
            }
        }
    }
}
