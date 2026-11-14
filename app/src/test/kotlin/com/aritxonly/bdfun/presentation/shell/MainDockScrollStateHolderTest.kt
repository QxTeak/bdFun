package com.aritxonly.bdfun

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainDockScrollStateHolderTest {
    @Test
    fun collapseAndRestoreRequireSixDpInTheCurrentDirection() {
        val state = MainDockScrollStateHolder(12f)
        fun scroll(y: Float) = state.onPostScroll(Offset(0f, y), Offset.Zero, NestedScrollSource.UserInput)
        scroll(-8f)
        assertFalse(state.collapsed)
        scroll(-4f)
        assertTrue(state.collapsed)
        scroll(8f)
        assertTrue(state.collapsed)
        scroll(4f)
        assertFalse(state.collapsed)
        scroll(-8f)
        scroll(2f)
        scroll(-4f)
        assertFalse(state.collapsed)
    }

    @Test
    fun flingHorizontalAndUnconsumedMotionDoNotCollapse() {
        val state = MainDockScrollStateHolder(6f)
        state.onPostScroll(Offset(0f, -40f), Offset.Zero, NestedScrollSource.SideEffect)
        state.onPostScroll(Offset(40f, 0f), Offset(0f, -40f), NestedScrollSource.UserInput)
        assertFalse(state.collapsed)
    }
}
