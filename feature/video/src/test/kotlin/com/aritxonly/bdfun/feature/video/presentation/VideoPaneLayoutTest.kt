package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class VideoPaneLayoutTest {
    @Test fun phoneLandscapeStaysStackedEvenWhenWindowIsWide() {
        val layout = resolveVideoPaneLayout(844f, 390f, smallestWidthDp = 393)
        assertFalse(layout.isSplit)
        assertEquals(844f, layout.playerWidthDp, 0.01f)
    }

    @Test fun tabletLandscapeReserves360BeforeRatioTakesOver() {
        assertEquals(360f, resolveVideoPaneLayout(1000f, 800f, 600).detailsWidthDp, 0.01f)
        assertEquals(360f, resolveVideoPaneLayout(1200f, 800f, 840).detailsWidthDp, 0.01f)
    }

    @Test fun largeTabletLandscapeUsesSeventyThirty() {
        val layout = resolveVideoPaneLayout(1280f, 800f, 840)
        assertEquals(384f, layout.detailsWidthDp, 0.01f)
        assertEquals(896f, layout.playerWidthDp, 0.01f)
    }

    @Test fun compactAndPortraitWindowsKeepStackedLayout() {
        assertFalse(resolveVideoPaneLayout(599f, 390f, 600).isSplit)
        assertFalse(resolveVideoPaneLayout(800f, 1280f, 840).isSplit)
        assertFalse(resolveVideoPaneLayout(800f, 800f, 840).isSplit)
        assertFalse(resolveVideoPaneLayout(800f, 0f, 840).isSplit)
    }

    @Test fun splitBoundaryReservesBothPanesWithoutOverflow() {
        for (width in listOf(600f, 720f, 999f, 1000f, 1920f)) {
            val layout = resolveVideoPaneLayout(width, 390f, 600)
            assertTrue(layout.detailsWidthDp >= 360f)
            assertTrue(layout.playerWidthDp >= 240f)
            assertEquals(width, layout.playerWidthDp + layout.detailsWidthDp, 0.01f)
        }
    }
}
