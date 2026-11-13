package com.aritxonly.bdfun.ui

/**
 * 共享元素插值的结果：以屏幕中心为轴缩放，再平移到缩略图中心。
 *
 * 全部用 Float 而不是 Compose 的 Rect，是为了能在纯 JVM 单测里验证，
 * 不依赖 Android 运行时（见 PhotoViewerGeometryTest）。
 */
internal data class PhotoGeometry(
    val scale: Float,
    val translationX: Float,
    val translationY: Float,
    val cornerRadius: Float,
)

/**
 * 把入场进度映射成图片变换。
 *
 * @param progress 0 = 完全落在缩略图矩形上；1 = 铺满全屏
 *
 * 缩放只用一个系数（宽比），行为与 Telegram 一致：X/Y 分开算会把图片压扁，
 * 这是之前动态页大图入场看起来「不像」的主因。
 */
internal fun photoGeometryAt(
    progress: Float,
    screenWidth: Float,
    screenHeight: Float,
    originX: Float,
    originY: Float,
    originWidth: Float,
    originHeight: Float,
    originRadius: Float,
): PhotoGeometry {
    val p = progress.coerceIn(0f, 1f)
    val startScale = if (screenWidth > 0f && originWidth > 0f) {
        (originWidth / screenWidth).coerceIn(0.05f, 1f)
    } else {
        0.9f
    }
    val centerX = originX + originWidth / 2f
    val centerY = originY + originHeight / 2f
    return PhotoGeometry(
        scale = startScale + (1f - startScale) * p,
        translationX = (centerX - screenWidth / 2f) * (1f - p),
        translationY = (centerY - screenHeight / 2f) * (1f - p),
        cornerRadius = (originRadius * (1f - p)).coerceAtLeast(0f),
    )
}

/** 下滑关闭的进度：拖到半屏就算走完全程，和 Telegram 的 |dy| / (屏高 / 2) 一致。 */
internal fun photoDismissProgress(dragY: Float, screenHeight: Float): Float =
    if (screenHeight <= 0f) 0f else (kotlin.math.abs(dragY) / (screenHeight / 2f)).coerceIn(0f, 1f)

/** 拖动时背后的页面缩到 0.9，产生「大图浮起来」的层次感。 */
internal fun photoBackdropScale(dismissProgress: Float): Float =
    1f - 0.1f * dismissProgress.coerceIn(0f, 1f)

/** 拖动时大图本身略微缩小，避免显得像一块贴纸。 */
internal fun photoDragScale(dismissProgress: Float): Float =
    1f - 0.12f * dismissProgress.coerceIn(0f, 1f)

/** 是否达到松手退出的阈值。距离和速度任一满足即可，和 Telegram 的判定一致。 */
internal fun photoShouldDismiss(
    dismissProgress: Float,
    velocityY: Float,
    progressThreshold: Float = 0.18f,
    velocityThreshold: Float = 900f,
): Boolean = dismissProgress > progressThreshold || kotlin.math.abs(velocityY) > velocityThreshold
