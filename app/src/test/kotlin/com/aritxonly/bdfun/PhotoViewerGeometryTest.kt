package com.aritxonly.bdfun.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoViewerGeometryTest {

    private val screenW = 1080f
    private val screenH = 2400f

    @Test fun fullProgressFillsScreenWithoutOffset() {
        val g = photoGeometryAt(
            progress = 1f, screenWidth = screenW, screenHeight = screenH,
            originX = 100f, originY = 200f, originWidth = 500f, originHeight = 300f,
            originRadius = 30f,
        )
        assertEquals(1f, g.scale, 0.0001f)
        assertEquals(0f, g.translationX, 0.0001f)
        assertEquals(0f, g.translationY, 0.0001f)
        assertEquals(0f, g.cornerRadius, 0.0001f)
    }

    @Test fun zeroProgressSitsOnThumbnailRect() {
        val g = photoGeometryAt(
            progress = 0f, screenWidth = screenW, screenHeight = screenH,
            originX = 100f, originY = 200f, originWidth = 540f, originHeight = 300f,
            originRadius = 30f,
        )
        // 单一缩放系数：按宽度比，X/Y 相同，图片不会被压扁
        assertEquals(0.5f, g.scale, 0.0001f)
        // 缩放后中心对齐缩略图中心：(370, 350) 与屏幕中心之差
        assertEquals(370f - screenW / 2f, g.translationX, 0.0001f)
        assertEquals(350f - screenH / 2f, g.translationY, 0.0001f)
        assertEquals(30f, g.cornerRadius, 0.0001f)
    }

    @Test fun progressIsClamped() {
        val below = photoGeometryAt(-1f, screenW, screenH, 0f, 0f, 540f, 300f, 12f)
        val above = photoGeometryAt(2f, screenW, screenH, 0f, 0f, 540f, 300f, 12f)
        assertEquals(photoGeometryAt(0f, screenW, screenH, 0f, 0f, 540f, 300f, 12f), below)
        assertEquals(photoGeometryAt(1f, screenW, screenH, 0f, 0f, 540f, 300f, 12f), above)
    }

    @Test fun dismissProgressUsesHalfScreen() {
        assertEquals(0f, photoDismissProgress(0f, screenH), 0.0001f)
        assertEquals(0.5f, photoDismissProgress(screenH / 4f, screenH), 0.0001f)
        assertEquals(1f, photoDismissProgress(screenH, screenH), 0.0001f)
        assertEquals(0.5f, photoDismissProgress(-screenH / 4f, screenH), 0.0001f)
        assertEquals(0f, photoDismissProgress(100f, 0f), 0.0001f)
    }

    @Test fun backdropZoomsInOnOpenAndReturnsOnDrag() {
        assertEquals(1f, photoBackdropScaleAt(0f, 0f), 0.0001f)
        assertEquals(1f + PhotoViewerBackdropZoomIn, photoBackdropScaleAt(1f, 0f), 0.0001f)
        assertEquals(1.05f, photoBackdropScaleAt(1f, 0.5f), 0.0001f)
        assertEquals(1f, photoBackdropScaleAt(1f, 1f), 0.0001f)
    }

    @Test fun dismissalNeedsDistanceOrSpeed() {
        assertFalse(photoShouldDismiss(dismissProgress = 0.1f, velocityY = 100f))
        assertTrue(photoShouldDismiss(dismissProgress = 0.2f, velocityY = 0f))
        assertTrue(photoShouldDismiss(dismissProgress = 0.01f, velocityY = 1000f))
        assertTrue(photoShouldDismiss(dismissProgress = 0.01f, velocityY = -1000f))
    }
}
