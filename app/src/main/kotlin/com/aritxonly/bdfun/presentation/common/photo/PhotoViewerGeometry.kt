package com.aritxonly.bdfun.ui

/**
 * 图片查看器的纯 JVM 数学部分。
 *
 * 全部用 Float 而不是 Compose 的 Rect，是为了能在单测里直接验证，
 * 不依赖 Android 运行时（见 PhotoViewerGeometryTest）。
 * 入场/退场的「裁剪框 + 图片倍率」插值在 PhotoViewerMorph.kt。
 */

/** 下滑关闭的进度：拖到半屏就算走完全程，和 Telegram 的 |dy| / (屏高 / 2) 一致。 */
internal fun photoDismissProgress(dragY: Float, screenHeight: Float): Float =
    if (screenHeight <= 0f) 0f else (kotlin.math.abs(dragY) / (screenHeight / 2f)).coerceIn(0f, 1f)

/** 打开时背景放大的幅度。Telegram 关闭时容器缩到 0.9，反向即打开从 0.9 回到 1.0。 */
internal const val PhotoViewerBackdropZoomIn = 0.1f

/**
 * 底层页面的缩放：
 * - 打开：随入场进度从 1.0 放大到 1.0 + ZoomIn（实测约 1.1）；
 * - 跟手拖动：按拖动进度从放大值退回 1.0，不是继续缩小到 0.9。
 * 两条时间线相减，关闭过程中两者都会回到 0，最终稳定在 1.0。
 */
internal fun photoBackdropScaleAt(progress: Float, dismissProgress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val d = dismissProgress.coerceIn(0f, 1f)
    return 1f + PhotoViewerBackdropZoomIn * (p - d)
}

/** 是否达到松手退出的阈值。距离和速度任一满足即可，和 Telegram 的判定一致。 */
internal fun photoShouldDismiss(
    dismissProgress: Float,
    velocityY: Float,
    progressThreshold: Float = 0.18f,
    velocityThreshold: Float = 900f,
): Boolean = dismissProgress > progressThreshold || kotlin.math.abs(velocityY) > velocityThreshold
