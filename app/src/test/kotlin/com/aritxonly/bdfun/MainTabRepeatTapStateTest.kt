package com.aritxonly.bdfun

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabRepeatTapStateTest {
    @Test fun firstTapReturnsToTopAndSecondTapRefreshesOnce() {
        val state = MainTabRepeatTapState()
        assertFalse(state.tap("home:0", 1000))
        assertTrue(state.tap("home:0", 1200))
        assertFalse(state.tap("home:0", 1300))
    }

    @Test fun slowTapsAndDifferentPagesDoNotRefresh() {
        val state = MainTabRepeatTapState()
        assertFalse(state.tap("home:0", 1000))
        assertFalse(state.tap("home:0", 1301))
        assertFalse(state.tap("dynamic", 1400))
        assertFalse(state.tap("home:1", 1500))
    }

    @Test fun NavigationResetsDoubleTapWindow() {
        val state = MainTabRepeatTapState()
        state.tap("home:0", 1000)
        state.reset()
        assertFalse(state.tap("home:0", 1100))
    }
}
