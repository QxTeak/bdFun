package com.aritxonly.teak.common.chrome

import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressiveTopBarHeightTest {
    @Test fun belowPlayerHeaderDoesNotExtendIntoTheNextContentRow() {
        val metrics = resolveProgressiveTopBarHeightMetrics(52, 0, 56, materialHeightTrimPx = 18)
        assertTrue(metrics.materialHeightPx in 52..60)
    }

    @Test fun screenTopHeaderStillCoversStatusBarAndControls() {
        val metrics = resolveProgressiveTopBarHeightMetrics(80, 28, 56, materialHeightTrimPx = 18)
        assertTrue(metrics.materialHeightPx >= 84)
    }
}
