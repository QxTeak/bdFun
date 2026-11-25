package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoViewerRotationTest {

    private val center = Offset(600f, 1200f)

    /** 未旋转时，点在中心右下方，放大后要往左上移，才能把那个点带回中心。 */
    @Test fun doubleTapPanMovesPointToCenter() {
        val pan = doubleTapZoomPan(tap = Offset(900f, 1500f), center = center, targetZoom = 2.5f)
        assertEquals(-750f, pan.x, 0.01f)
        assertEquals(-750f, pan.y, 0.01f)
    }

    /** 点正好在中心：不需要平移。 */
    @Test fun doubleTapAtCenterKeepsStill() {
        val pan = doubleTapZoomPan(tap = center, center = center, targetZoom = 2.5f)
        assertEquals(0f, pan.x, 0.01f)
        assertEquals(0f, pan.y, 0.01f)
    }

    /** 屏幕位移先乘倍率再反向：点在中心左侧 100，放大 2.4 倍后要右移 240。 */
    @Test fun doubleTapPanScalesWithZoom() {
        val pan = doubleTapZoomPan(tap = Offset(500f, 1200f), center = center, targetZoom = 2.4f)
        assertEquals(240f, pan.x, 0.01f)
        assertEquals(0f, pan.y, 0.01f)
    }

    /** 取消放大时回到原位。 */
    @Test fun resetZoomHasNoPan() {
        val pan = doubleTapZoomPan(tap = Offset(900f, 1500f), center = center, targetZoom = 1f)
        assertEquals(0f, pan.x, 0.01f)
        assertEquals(0f, pan.y, 0.01f)
    }

    @Test fun rotationFitScaleOnlyShrinksTallImages() {
        // 16:9 横图转 90° 后仍在屏幕里，不需要再缩。
        assertEquals(1f, photoRotationFitScale(1, 1920f, 1080f, 1080f, 2400f), 0.0001f)
        // 竖图转 90° 后宽度 = 原高度 2400 > 屏幕 1080，按比例缩到 0.45。
        assertEquals(0.45f, photoRotationFitScale(1, 1080f, 2400f, 1080f, 2400f), 0.0001f)
        // 未旋转时永远是 1。
        assertEquals(1f, photoRotationFitScale(0, 1080f, 2400f, 1080f, 2400f), 0.0001f)
    }

    @Test fun fitScaleStaysPositiveForUnknownSizes() {
        assertTrue(photoRotationFitScale(3, 0f, 0f, 0f, 0f) == 1f)
    }
}
