package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.DanmakuItem
import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuFilterOrderTest {
    private val options = DanmakuFilterOptions(false, false, false, false, false, false, false, emptySet())
    private fun item(time: Long, text: String) = DanmakuItem(time, 1, 0xFFFFFF, 1f, text)
    @Test fun sortedAndUnsortedInputsKeepStableTimeOrder() {
        val first = item(10, "first")
        val second = item(10, "second")
        val last = item(20, "last")
        val expected = listOf(first, second, last)
        assertEquals(expected, filterDanmaku(expected, options))
        assertEquals(expected, filterDanmaku(listOf(last, first, second), options))
        assertEquals(emptyList<DanmakuItem>(), filterDanmaku(emptyList(), options))
    }
}
