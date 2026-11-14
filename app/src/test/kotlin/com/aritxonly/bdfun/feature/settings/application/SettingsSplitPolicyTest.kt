package com.aritxonly.bdfun.feature.settings.application

import org.junit.Assert.*
import org.junit.Test

class SettingsSplitPolicyTest {
    @Test fun defaultsToThreeToSevenWith360Minimum() {
        assertEquals(480f, SettingsSplitPolicy.leftWidth(1612f), .001f)
        assertEquals(360f, SettingsSplitPolicy.leftWidth(1012f), .001f)
        assertEquals(360f, SettingsSplitPolicy.leftWidth(612f), .001f)
    }

    @Test fun dividerTracksDragInsteadOfJumpingFromUnclampedRatio() {
        val moved = SettingsSplitPolicy.drag(1012f, .3f, 100f)
        assertEquals(460f, SettingsSplitPolicy.leftWidth(1012f, moved), .001f)
        val minimum = SettingsSplitPolicy.drag(1012f, moved, -1000f)
        assertEquals(360f, SettingsSplitPolicy.leftWidth(1012f, minimum), .001f)
    }

    @Test fun rightPaneAlwaysGetsRemainingWidth() {
        listOf(612f, 922f, 1612f).forEach { total ->
            listOf(-1f, .3f, .6f, 2f).forEach { ratio ->
                val left = SettingsSplitPolicy.leftWidth(total, ratio)
                assertTrue(left >= 360f)
                assertTrue(total - SettingsSplitPolicy.DividerDp - left >= 1f)
            }
        }
    }

    @Test fun invalidFractionUsesDefaultAndTinyWindowsDoNotCrash() {
        assertEquals(SettingsSplitPolicy.leftWidth(1612f), SettingsSplitPolicy.leftWidth(1612f, Float.NaN), 0f)
        assertEquals(0f, SettingsSplitPolicy.leftWidth(0f), 0f)
    }
}
