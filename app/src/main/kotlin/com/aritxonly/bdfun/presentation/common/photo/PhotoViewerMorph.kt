package com.aritxonly.bdfun.ui

/**
 * 一帧「比例展开」的结果。
 *
 * 和单纯缩放不同，这里把两件事分开：
 * - clip 矩形：从缩略图矩形插值到全屏里图片的实际矩形；
 * - imageScale：从「铺满缩略图框」的裁切倍率插值到 1（完整可见）。
 *
 * p=0 时，图片被放大到刚好铺满缩略图框，再裁掉框外的部分——画面和卡片上看到的
 * Crop 完全一致；p=1 时倍率为 1、裁剪框就是图片矩形，完整图可见。中间过程中
 * 被裁掉的内容会自然展开，不会变形（倍率始终是等比的）。
 */
internal data class PhotoMorphFrame(
    val clipLeft: Float,
    val clipTop: Float,
    val clipWidth: Float,
    val clipHeight: Float,
    val imageWidth: Float,
    val imageHeight: Float,
    val imageCenterX: Float,
    val imageCenterY: Float,
    val imageScale: Float,
    val cornerRadius: Float,
)

internal fun photoMorphFrame(
    progress: Float,
    originX: Float,
    originY: Float,
    originWidth: Float,
    originHeight: Float,
    screenWidth: Float,
    screenHeight: Float,
    imageAspect: Float,
    originRadius: Float,
): PhotoMorphFrame {
    val p = progress.coerceIn(0f, 1f)
    val aspect = if (imageAspect > 0.01f) imageAspect else screenWidth / screenHeight
    // 全屏里图片的实际矩形（Contain）
    val imageWidth = if (screenWidth / screenHeight > aspect) screenHeight * aspect else screenWidth
    val imageHeight = imageWidth / aspect
    val imageLeft = (screenWidth - imageWidth) / 2f
    val imageTop = (screenHeight - imageHeight) / 2f

    val startWidth = if (originWidth > 1f) originWidth else screenWidth
    val startHeight = if (originHeight > 1f) originHeight else screenHeight
    val startCenterX = originX + startWidth / 2f
    val startCenterY = originY + startHeight / 2f
    val endCenterX = imageLeft + imageWidth / 2f
    val endCenterY = imageTop + imageHeight / 2f

    // p=0 需要铺满缩略图框，取两个方向里更大的那个倍率
    val coverScale = maxOf(startWidth / imageWidth, startHeight / imageHeight)
    val scale = coverScale + (1f - coverScale) * p

    val centerX = startCenterX + (endCenterX - startCenterX) * p
    val centerY = startCenterY + (endCenterY - startCenterY) * p
    val clipWidth = startWidth + (imageWidth - startWidth) * p
    val clipHeight = startHeight + (imageHeight - startHeight) * p

    return PhotoMorphFrame(
        clipLeft = centerX - clipWidth / 2f,
        clipTop = centerY - clipHeight / 2f,
        clipWidth = clipWidth,
        clipHeight = clipHeight,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        imageCenterX = centerX,
        imageCenterY = centerY,
        imageScale = scale,
        cornerRadius = (originRadius * (1f - p)).coerceAtLeast(0f),
    )
}
