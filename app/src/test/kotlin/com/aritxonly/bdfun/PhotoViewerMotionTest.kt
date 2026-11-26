package com.aritxonly.bdfun.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoViewerMotionTest {

    @Test
    fun endpointsAreExact() {
        assertEquals(0f, PhotoViewerMotion.progressAt(0f, PhotoViewerMotion.OpenDurationMs), 0f)
        assertEquals(1f, PhotoViewerMotion.progressAt(400f, PhotoViewerMotion.OpenDurationMs), 0f)
        assertEquals(1f, PhotoViewerMotion.progressAt(500f, PhotoViewerMotion.OpenDurationMs), 0f)
    }

    @Test
    fun disabledAnimationJumpsToEnd() {
        assertEquals(1f, PhotoViewerMotion.progressAt(1f, 0L), 0f)
    }

    @Test
    fun curveIsMonotonicAndNeverExceedsOne() {
        var previous = 0f
        for (ms in 0..PhotoViewerMotion.OpenDurationMs.toInt()) {
            val value = PhotoViewerMotion.progressAt(ms.toFloat(), PhotoViewerMotion.OpenDurationMs)
            assertTrue("ms=$ms value=$value", value >= previous - 0.0001f)
            assertTrue("ms=$ms value=$value", value <= 1f)
            previous = value
        }
    }

    @Test
    fun frontLoadsMostOfTheDistance() {
        // 搜索那套弹簧前段很快：一半时长已经走完八成以上，这是和线性/AccelerateDecelerate 的关键区别。
        val third = PhotoViewerMotion.progressAt(133f, PhotoViewerMotion.OpenDurationMs)
        val half = PhotoViewerMotion.progressAt(200f, PhotoViewerMotion.OpenDurationMs)
        assertTrue("third=$third", third > 0.58f)
        assertTrue("half=$half", half > 0.82f)
        // 后段逐渐收住，最后 50ms 只补最后一点。
        val last = PhotoViewerMotion.progressAt(350f, PhotoViewerMotion.OpenDurationMs)
        assertTrue("last=$last", last > 0.98f)
    }

    @Test
    fun closeUsesTheSameCurveAtANarrowerWindow() {
        val closeMid = PhotoViewerMotion.progressAt(150f, PhotoViewerMotion.CloseDurationMs)
        val openMid = PhotoViewerMotion.progressAt(150f, PhotoViewerMotion.OpenDurationMs)
        assertTrue("closeMid=$closeMid", closeMid > 0.7f)
        assertTrue("close should be ahead of open at the same elapsed time", closeMid > openMid)
    }
}
