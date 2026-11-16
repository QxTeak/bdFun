package com.bytedance.danmaku.render.engine.render.layer.scroll

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrollReservedTopTest {
    @Test
    fun zeroGuardNeverReserves() {
        assertFalse(isLineReservedByTopGuard(0f, 0f))
        assertFalse(isLineReservedByTopGuard(120f, 0f))
    }

    @Test
    fun linesStartingInsideGuardAreReserved() {
        assertTrue(isLineReservedByTopGuard(0f, 126f))
        assertTrue(isLineReservedByTopGuard(68f, 126f))
    }

    @Test
    fun lineStartingAtOrBelowGuardStaysUsable() {
        assertFalse(isLineReservedByTopGuard(126f, 126f))
        assertFalse(isLineReservedByTopGuard(194f, 126f))
    }

    @Test
    fun guardDisappearingImmediatelyRestoresTopLines() {
        assertTrue(isLineReservedByTopGuard(0f, 126f))
        assertFalse(isLineReservedByTopGuard(0f, 0f))
    }
}
