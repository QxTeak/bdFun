package com.aritxonly.bdfun.presentation.pgc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiFanLayoutTest {

    private fun slots(focus: Float, count: Int = 6): List<Float> =
        (0 until count).map { bangumiFanSlot(it, focus, count) }

    @Test
    fun restFocusShowsFiveCardsAndKeepsOneWaiting() {
        // 焦点 0：0 在中间，1/2 在右边，5/4 在左边，3 停在 +3 的隐藏位。
        assertEquals(listOf(0f, 1f, 2f, 3f, -2f, -1f), slots(0f))
        assertEquals(5, slots(0f).count { kotlin.math.abs(it) <= BANGUMI_FAN_VISIBLE_RADIUS })
    }

    @Test
    fun forwardFocusMovesEveryCardOneSlotLeft() {
        assertEquals(listOf(-1f, 0f, 1f, 2f, 3f, -2f), slots(1f))
        // 半个槽位时左侧最外的卡走到 -2.5，下一帧起环绕到右侧隐藏位。
        assertEquals(-2.5f, bangumiFanSlot(4, 0.5f, 6), 0.001f)
    }

    @Test
    fun backwardFocusMovesEveryCardOneSlotRight() {
        // 往回切时隐藏的那张从左侧入场，右侧最外的卡退到隐藏位。
        assertEquals(listOf(1f, 2f, 3f, -2f, -1f, 0f), slots(-1f))
    }

    @Test
    fun alphaIsOneInsideVisibleAreaAndZeroOutside() {
        assertEquals(1f, bangumiFanAlpha(0f), 0.001f)
        assertEquals(1f, bangumiFanAlpha(2f), 0.001f)
        assertEquals(1f, bangumiFanAlpha(-2f), 0.001f)
        assertEquals(0f, bangumiFanAlpha(3f), 0.001f)
        // 环绕发生在 -2.5/+3.5，此处已经淡到 0，看不到跳变。
        assertEquals(0f, bangumiFanAlpha(-2.5f), 0.001f)
        val half = bangumiFanAlpha((BANGUMI_FAN_FADE_START + BANGUMI_FAN_FADE_END) / 2f)
        assertTrue(half > 0f && half < 1f)
    }

    @Test
    fun sideCardsTiltInwardAndShrink() {
        // 左侧卡绕竖轴正向转，内（右）边朝屏幕里收；右侧反号，两边都往中间倒。
        assertTrue(bangumiFanRotation(-1f) > 0f)
        assertTrue(bangumiFanRotation(1f) < 0f)
        assertEquals(0f, bangumiFanRotation(0f), 0.001f)
        assertTrue(bangumiFanScale(1f) < bangumiFanScale(0f))
        assertTrue(bangumiFanScale(-2f) < bangumiFanScale(-1f))
    }
}
