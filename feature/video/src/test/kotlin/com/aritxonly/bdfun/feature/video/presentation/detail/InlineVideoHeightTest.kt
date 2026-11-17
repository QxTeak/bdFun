package com.aritxonly.bdfun.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InlineVideoHeightTest {
    @Test
    fun landscapeOrUnknownAspectKeepsSixteenByNine() {
        assertEquals(360f * 9f / 16f, resolveInlineVideoHeightDp(360f, 800f, 16f / 9f), 0.001f)
        assertEquals(360f * 9f / 16f, resolveInlineVideoHeightDp(360f, 800f, null), 0.001f)
        assertEquals(360f * 9f / 16f, resolveInlineVideoHeightDp(360f, 800f, 1f), 0.001f)
    }

    @Test
    fun portraitVideoUsesTallerThreeByFourBlock() {
        // 窄屏：3:4 = 480，低于 60% 屏高（600），取 480。
        assertEquals(480f, resolveInlineVideoHeightDp(360f, 1000f, 9f / 16f), 0.001f)
    }

    @Test
    fun portraitVideoIsCappedByWindowHeight() {
        // 小屏：3:4 = 360，但屏高 60% 只有 300，取 300，避免顶掉整页。
        assertEquals(300f, resolveInlineVideoHeightDp(270f, 500f, 0.5625f), 0.001f)
    }

    @Test
    fun zeroWindowHeightFallsBackToTheTallBlock() {
        assertEquals(480f, resolveInlineVideoHeightDp(360f, 0f, 0.5625f), 0.001f)
    }
}
