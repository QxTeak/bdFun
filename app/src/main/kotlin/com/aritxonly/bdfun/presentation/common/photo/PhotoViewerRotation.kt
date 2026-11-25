package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.min

/**
 * 双击放大到 [targetZoom] 后，让手指点的那个位置落在画面中心所需要的平移量。
 *
 * 缩放和旋转都绕图片自身中心，图片中心在未放大时就重合在画面中心；
 * 一个离中心 [tap] 的点在放大后仍在同一条半径上、只是被拉远，所以平移量就是
 * 「反向的位移 × 倍率」，不需要再按旋转角换算。
 */
internal fun doubleTapZoomPan(
    tap: Offset,
    center: Offset,
    targetZoom: Float,
): Offset {
    if (targetZoom <= 1.01f) return Offset.Zero
    return Offset((center.x - tap.x) * targetZoom, (center.y - tap.y) * targetZoom)
}

/**
 * 旋转后仍要装进屏幕，需要在缩放上再乘的系数。
 * 只缩小不放大：竖图转成横向时宽度会超过屏幕，横图转过来本来就更小，不必再撑满。
 */
internal fun photoRotationFitScale(
    rotationQuarter: Int,
    imageWidth: Float,
    imageHeight: Float,
    screenWidth: Float,
    screenHeight: Float,
): Float {
    if (rotationQuarter % 2 == 0) return 1f
    if (imageWidth <= 0f || imageHeight <= 0f || screenWidth <= 0f || screenHeight <= 0f) return 1f
    return min(min(screenWidth / imageHeight, screenHeight / imageWidth), 1f)
}
