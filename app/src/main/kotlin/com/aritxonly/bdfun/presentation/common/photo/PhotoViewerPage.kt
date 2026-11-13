package com.aritxonly.bdfun.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import kotlin.math.abs

/**
 * 大图单页：双指缩放 + 双击放大 + 放大后平移。
 *
 * 手势规则和原来的动态页保持一致：原始比例时单指横向拖动不消费，
 * 交给外层 Pager 翻页；只有双指或已放大时才自己处理。
 */
@Composable
internal fun PhotoViewerPage(url: String, onScaleChanged: (Float) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(scale) { onScaleChanged(scale) }

    AsyncImage(
        model = rememberImageRequest(url),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            // 以双击点为轴：把点拉到屏幕中心，避免放大后跑到画面外
                            val c = Offset(size.width / 2f, size.height / 2f)
                            offset = (c - tap) * (scale - 1f)
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val next = (scale * zoomChange).coerceIn(1f, 4f)
                            scale = next
                            offset = if (next > 1.01f) clampOffset(offset + panChange, next, size) else Offset.Zero
                            event.changes.forEach { it.consume() }
                        } else if (scale > 1.01f) {
                            offset = clampOffset(offset + event.calculatePan(), scale, size)
                            event.changes.forEach { it.consume() }
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

/** 放大后不让图片被拖出屏幕：允许的最大位移是超出部分的一半。 */
private fun clampOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (size.width == 0 || size.height == 0) return offset
    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f
    return Offset(
        x = offset.x.coerceIn(-abs(maxX), abs(maxX)),
        y = offset.y.coerceIn(-abs(maxY), abs(maxY)),
    )
}
