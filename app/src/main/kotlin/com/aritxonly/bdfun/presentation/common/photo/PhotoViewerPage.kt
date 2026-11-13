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
 * 大图单页：双指缩放、双击放大、放大后平移。
 *
 * 交互照 Telegram：原始比例时单指横向拖动不消费，交给外层 Pager 翻页；
 * 只有双指或已放大才自己处理。单击切换工具栏显隐，双击在 1x 和 2.5x 之间切换。
 */
@Composable
internal fun PhotoViewerPage(url: String, view: PhotoViewerTransitionState) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(scale) { view.zoom = scale }

    AsyncImage(
        model = rememberImageRequest(url),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { view.chromeVisible = !view.chromeVisible },
                    onDoubleTap = { tap ->
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            offset = (center - tap) * (scale - 1f)
                        }
                    },
                )
            }
            .photoZoomGestures(
                onScale = { scale = it },
                onOffset = { offset = it },
                currentScale = { scale },
                currentOffset = { offset },
                size = { size },
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

/** 双指缩放/平移。原始比例下的单指横向拖动不消费，交给 Pager 翻页。 */
private fun Modifier.photoZoomGestures(
    onScale: (Float) -> Unit,
    onOffset: (Offset) -> Unit,
    currentScale: () -> Float,
    currentOffset: () -> Offset,
    size: () -> IntSize,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed >= 2) {
                val next = (currentScale() * event.calculateZoom()).coerceIn(1f, 4f)
                onScale(next)
                onOffset(
                    if (next > 1.01f) {
                        clampOffset(currentOffset() + event.calculatePan(), next, size())
                    } else {
                        Offset.Zero
                    },
                )
                event.changes.forEach { it.consume() }
            } else if (currentScale() > 1.01f) {
                onOffset(clampOffset(currentOffset() + event.calculatePan(), currentScale(), size()))
                event.changes.forEach { it.consume() }
            }
            if (event.changes.none { it.pressed }) break
        }
    }
}

/** 放大后不让图片被拖出屏幕：允许的最大位移是超出部分的一半。 */
private fun clampOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (size.width == 0 || size.height == 0) return offset
    val maxX = abs(size.width * (scale - 1f)) / 2f
    val maxY = abs(size.height * (scale - 1f)) / 2f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}
