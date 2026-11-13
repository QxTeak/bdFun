package com.aritxonly.bdfun.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoViewerGeometryTest {

    private val screenW = 1080f
    private val screenH = 2400f

    /** 4:3 横图：全屏 Contain 后宽 1080、高 810，上下各留 795。 */
    private val landscape = 4f / 3f

    private fun landscapeFrame(progress: Float) = photoMorphFrame(
        progress = progress,
        originX = 100f,
        originY = 200f,
        originWidth = 540f,
        originHeight = 300f,
        screenWidth = screenW,
        screenHeight = screenH,
        imageAspect = landscape,
        originRadius = 30f,
    )

    @Test fun zeroProgressSitsOnThumbnailRect() {
        val f = landscapeFrame(0f)
        // 裁剪框与卡片完全重合
        assertEquals(100f, f.clipLeft, 0.001f)
        assertEquals(200f, f.clipTop, 0.001f)
        assertEquals(540f, f.clipWidth, 0.001f)
        assertEquals(300f, f.clipHeight, 0.001f)
        // 图片要铺满这个框：按较长的一边算倍率（540/1080 = 300/810 = 0.5）
        assertEquals(0.5f, f.imageScale, 0.001f)
        assertEquals(370f, f.imageCenterX, 0.001f)
        assertEquals(350f, f.imageCenterY, 0.001f)
        assertEquals(30f, f.cornerRadius, 0.001f)
    }

    @Test fun fullProgressShowsWholeImage() {
        val f = landscapeFrame(1f)
        // 裁剪框就是图片在全屏里 Contain 后的矩形
        assertEquals(0f, f.clipLeft, 0.001f)
        assertEquals(795f, f.clipTop, 0.001f)
        assertEquals(1080f, f.clipWidth, 0.001f)
        assertEquals(810f, f.clipHeight, 0.001f)
        // 倍率为 1，不裁切、不变形
        assertEquals(1f, f.imageScale, 0.001f)
        assertEquals(540f, f.imageCenterX, 0.001f)
        assertEquals(1200f, f.imageCenterY, 0.001f)
        assertEquals(0f, f.cornerRadius, 0.001f)
    }

    @Test fun middleFrameStaysProportional() {
        val f = landscapeFrame(0.5f)
        // 图片本身的尺寸不随进度变化，只有倍率变化——所以等比，永远不会被压扁
        assertEquals(1080f, f.imageWidth, 0.001f)
        assertEquals(810f, f.imageHeight, 0.001f)
        assertEquals(0.75f, f.imageScale, 0.001f)
        assertEquals(50f, f.clipLeft, 0.001f)
        assertEquals(497.5f, f.clipTop, 0.001f)
        assertEquals(810f, f.clipWidth, 0.001f)
        assertEquals(555f, f.clipHeight, 0.001f)
        assertEquals(15f, f.cornerRadius, 0.001f)
    }

    @Test fun portraitImageUsesWidthWhenTall() {
        // 9:16 竖图比屏幕更窄：宽度撑满 1080，高度按比例到 1920
        val f = photoMorphFrame(
            progress = 1f,
            originX = 0f, originY = 0f, originWidth = 540f, originHeight = 960f,
            screenWidth = screenW, screenHeight = screenH,
            imageAspect = 9f / 16f,
            originRadius = 0f,
        )
        assertEquals(1080f, f.clipWidth, 0.01f)
        assertEquals(1920f, f.clipHeight, 0.01f)
        assertEquals(0f, f.clipLeft, 0.01f)
        assertEquals(240f, f.clipTop, 0.01f)
    }

    @Test fun progressIsClamped() {
        assertEquals(landscapeFrame(0f), landscapeFrame(-1f))
        assertEquals(landscapeFrame(1f), landscapeFrame(2f))
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

    @Test fun backdropNeverShrinksBelowNormal() {
        // 松手退出时 dragY 是冻结的，进度还在往 0 走；底层只能回到 1.0，不能缩到 0.9。
        for (p in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            for (d in listOf(0f, 0.4f, 0.8f, 1f)) {
                assertTrue("p=$p d=$d", photoBackdropScaleAt(p, d) >= 1f - 0.0001f)
            }
        }
        assertEquals(1f, photoBackdropScaleAt(0f, 1f), 0.0001f)
        assertEquals(1.1f, photoBackdropScaleAt(1f, 0f), 0.0001f)
    }

    @Test fun dismissalNeedsDistanceOrSpeed() {
        assertFalse(photoShouldDismiss(dismissProgress = 0.1f, velocityY = 100f))
        assertTrue(photoShouldDismiss(dismissProgress = 0.2f, velocityY = 0f))
        assertTrue(photoShouldDismiss(dismissProgress = 0.01f, velocityY = 1000f))
        assertTrue(photoShouldDismiss(dismissProgress = 0.01f, velocityY = -1000f))
    }
}
