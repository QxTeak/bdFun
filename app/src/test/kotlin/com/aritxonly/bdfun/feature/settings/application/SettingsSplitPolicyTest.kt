package com.aritxonly.bdfun.feature.settings.application

import org.junit.Assert.*
import org.junit.Test

class SettingsSplitPolicyTest {
    @Test fun defaultsToThreeToSevenWith360Minimum() {
        assertEquals(480f, SettingsSplitPolicy.leftWidth(1600f), .001f)
        assertEquals(360f, SettingsSplitPolicy.leftWidth(1000f), .001f)
        assertEquals(360f, SettingsSplitPolicy.leftWidth(600f), .001f)
    }
    @Test fun fixedPanesUseWholeWindowWithoutDivider() {
        listOf(600f, 922f, 1600f).forEach { total ->
            val left = SettingsSplitPolicy.leftWidth(total)
            assertTrue(left >= 360f)
            assertTrue(total - left >= 240f)
            assertEquals(total, left + (total - left), 0f)
        }
        assertEquals(0f, SettingsSplitPolicy.leftWidth(0f), 0f)
    }
}
