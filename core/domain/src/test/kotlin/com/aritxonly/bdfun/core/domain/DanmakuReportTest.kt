package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class DanmakuReportTest {
    @Test fun otherRequiresExplanationAndIdsMustBePositive() {
        assertFalse(DanmakuReport(1, 2, DanmakuReportReason.Other, "  ").valid)
        assertTrue(DanmakuReport(1, 2, DanmakuReportReason.Other, "说明").valid)
        assertTrue(DanmakuReport(1, 2, DanmakuReportReason.Spam).valid)
        assertFalse(DanmakuReport(0, 2, DanmakuReportReason.Spam).valid)
        assertFalse(DanmakuReport(1, 0, DanmakuReportReason.Spam).valid)
    }
    @Test fun referenceReasonCodesAreUniqueAndOtherIsEleven() {
        assertEquals(11, DanmakuReportReason.Other.code)
        assertEquals((1..13).toSet(), DanmakuReportReason.entries.map { it.code }.toSet())
    }
}
