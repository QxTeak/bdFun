package com.aritxonly.teak.common.chrome

import org.junit.Assert.*
import org.junit.Test

class MaterialProgressTest {
    @Test fun revealTracksAbsolutePositionAndReturnsToTransparent() {
        val state = TeakPageScrollState()
        state.update(0, 32f, density = 2f)
        assertEquals(0.5f, state.materialProgress, 0.0001f)
        state.update(5, 0f, density = 2f)
        assertEquals(1f, state.materialProgress, 0f)
        state.update(0, 0f, density = 2f)
        assertEquals(0f, state.materialProgress, 0f)
    }
    @Test fun pagesDoNotShareProgress() {
        val home = TeakPageScrollState()
        val dynamic = TeakPageScrollState()
        home.update(1, 0f)
        dynamic.reset()
        assertEquals(1f, home.materialProgress, 0f)
        assertEquals(0f, dynamic.materialProgress, 0f)
    }
    @Test fun revealIsClampedAndMonotonic() {
        assertEquals(0f, resolveTeakMaterialProgress(-100f), 0f)
        assertEquals(0f, resolveTeakMaterialProgress(4f), 0f)
        assertEquals(1f, resolveTeakMaterialProgress(28f), 0f)
        assertEquals(1f, resolveTeakMaterialProgress(10000f), 0f)
        for (i in 0..30) assertTrue(resolveTeakMaterialProgress(i.toFloat()) <= resolveTeakMaterialProgress(i + 1f))
    }
}
