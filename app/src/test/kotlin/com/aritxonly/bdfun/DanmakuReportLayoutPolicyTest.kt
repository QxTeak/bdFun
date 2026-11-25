package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.domain.DanmakuReportReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuReportLayoutPolicyTest {
    @Test fun wideLandscapeUsesFourColumnsToKeepReasonTextReadable() {
        val layout = danmakuReportLayout(
            landscape = true,
            screenWidthDp = 800,
            screenHeightDp = 360,
        )
        assertEquals(4, layout.columns)
        assertTrue(layout.compactButtons)
        assertEquals(4, DanmakuReportReason.entries.chunked(layout.columns).size)
        assertTrue((layout.maxContentHeightDp ?: 0) in 140..240)
        assertEquals(2, layout.summaryMaxLines)
    }

    @Test fun narrowLandscapeStillBoundsContentForScrolling() {
        val layout = danmakuReportLayout(
            landscape = true,
            screenWidthDp = 560,
            screenHeightDp = 320,
        )
        assertEquals(3, layout.columns)
        assertTrue(layout.compactButtons)
        assertTrue((layout.maxContentHeightDp ?: 0) >= 140)
        assertEquals(2, layout.summaryMaxLines)
    }

    @Test fun portraitKeepsExistingThreeColumnDialog() {
        val layout = danmakuReportLayout(
            landscape = false,
            screenWidthDp = 400,
            screenHeightDp = 800,
        )
        assertEquals(3, layout.columns)
        assertFalse(layout.compactButtons)
        assertNull(layout.maxContentHeightDp)
        assertEquals(2, layout.summaryMaxLines)
    }
}
