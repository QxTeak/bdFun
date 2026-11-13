package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class FeedLayoutTest {
    @Test fun compactWindowsKeepAllAvailableSpace() {
        for (width in listOf(320f, 360f, 600f, 735f)) assertEquals(0f, dynamicSidebarWidth(width), 0f)
    }
    @Test fun foldableFitsTwoLanesBesideTheSidebar() {
        for (width in listOf(736f, 840f)) {
            val available = (width - dynamicSidebarWidth(width) - 16).toInt()
            val cells = boundedVideoCellSizes(available, 12, FeedCardMinWidth, FeedCardMaxWidth)
            assertEquals(2, cells.size)
            assertTrue(cells.all { it in FeedCardMinWidth..FeedCardMaxWidth })
            assertTrue(cells.sum() + 12 <= available)
        }
    }
    @Test fun largeWindowsAddLanesInsteadOfStretchingCards() {
        val width = 1920f
        val available = (width - dynamicSidebarWidth(width) - 16).toInt()
        val cells = boundedVideoCellSizes(available, 12, FeedCardMinWidth, FeedCardMaxWidth)
        assertTrue(cells.size > 2)
        assertTrue(cells.all { it in FeedCardMinWidth..FeedCardMaxWidth })
        assertTrue(cells.sum() + 12 * (cells.size - 1) <= available)
    }
}
