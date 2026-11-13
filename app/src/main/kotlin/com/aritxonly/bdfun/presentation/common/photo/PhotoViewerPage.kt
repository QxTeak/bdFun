package com.aritxonly.bdfun.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 大图单页：从缩略图的裁切画面展开成完整图片，并支持双指缩放、双击放大、放大后平移。
 *
 * 关键点和之前的实现不同：不是把整层统一缩放，而是每帧算「裁剪框 + 图片倍率」，
 * 起点与缩略图的 Crop 完全重合，终点是完整图，中间的过渡是被裁掉的部分自然展开。
 */
@Composable
internal fun PhotoViewerPage(
    url: String,
    request: PhotoViewerRequest,
    view: PhotoViewerTransitionState,
) {
    var aspect by remember(url) { mutableFloatStateOf(0f) }
    var zoom by remember(url) { mutableFloatStateOf(1f) }
    var pan by remember(url) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(zoom) { view.zoom = zoom }

    val density = LocalDensity.current
    val screenWidth = view.size.width.toFloat()
    val screenHeight = view.size.height.toFloat()
    if (screenWidth <= 0f || screenHeight <= 0f) return

    val origin = request.origin
    val morph = photoMorphFrame(
        progress = view.progress.value,
        originX = origin?.left ?: 0f,
        originY = origin?.top ?: 0f,
        originWidth = origin?.width ?: 0f,
        originHeight = origin?.height ?: 0f,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        imageAspect = aspect,
        originRadius = request.originRadiusPx,
    )
    // 拖动位移随进度衰减：进度归零时正好落在缩略图上，不会带着偏移落地再跳回去。
    val dragY = view.dragY * view.progress.value
    val cornerDp = with(density) { morph.cornerRadius.toDp() }
    val clipWidthDp = with(density) { morph.clipWidth.toDp() }
    val clipHeightDp = with(density) { morph.clipHeight.toDp() }
    val imageWidthDp = with(density) { morph.imageWidth.toDp() }
    val imageHeightDp = with(density) { morph.imageHeight.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { view.chromeVisible = !view.chromeVisible },
                    onDoubleTap = { tap -> zoom = toggleZoom(zoom, tap, view.size) },
                )
            }
            .photoZoomGestures(
                onZoom = { zoom = it },
                onPan = { pan = it },
                currentZoom = { zoom },
                currentPan = { pan },
                size = { view.size },
            )
            .graphicsLayer { translationY = dragY },
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(morph.clipLeft.roundToInt(), morph.clipTop.roundToInt()) }
                .size(clipWidthDp, clipHeightDp)
                .clip(RoundedCornerShape(cornerDp)),
        ) {
            AsyncImage(
                model = rememberImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    val size = state.painter.intrinsicSize
                    if (size.width > 0f && size.height > 0f) aspect = size.width / size.height
                },
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (morph.imageCenterX - morph.imageWidth / 2f - morph.clipLeft).roundToInt(),
                            (morph.imageCenterY - morph.imageHeight / 2f - morph.clipTop).roundToInt(),
                        )
                    }
                    .size(imageWidthDp, imageHeightDp)
                    .graphicsLayer {
                        val scale = morph.imageScale * zoom
                        scaleX = scale
                        scaleY = scale
                        translationX = pan.x
                        translationY = pan.y
                    },
            )
        }
    }
}

private fun toggleZoom(zoom: Float, tap: Offset, size: IntSize): Float {
    if (zoom > 1.01f) return 1f
    return if (size.width > 0 && size.height > 0) 2.5f else zoom
}

/** 双指缩放/平移。原始比例下的单指横向拖动不消费，交给 Pager 翻页。 */
private fun Modifier.photoZoomGestures(
    onZoom: (Float) -> Unit,
    onPan: (Offset) -> Unit,
    currentZoom: () -> Float,
    currentPan: () -> Offset,
    size: () -> IntSize,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed >= 2) {
                val next = (currentZoom() * event.calculateZoom()).coerceIn(1f, 4f)
                onZoom(next)
                onPan(if (next > 1.01f) clampPan(currentPan() + event.calculatePan(), next, size()) else Offset.Zero)
                event.changes.forEach { it.consume() }
            } else if (currentZoom() > 1.01f) {
                onPan(clampPan(currentPan() + event.calculatePan(), currentZoom(), size()))
                event.changes.forEach { it.consume() }
            }
            if (event.changes.none { it.pressed }) break
        }
    }
}

/** 放大后不让图片被拖出屏幕：允许的最大位移是超出部分的一半。 */
private fun clampPan(pan: Offset, zoom: Float, size: IntSize): Offset {
    if (size.width == 0 || size.height == 0) return pan
    val maxX = abs(size.width * (zoom - 1f)) / 2f
    val maxY = abs(size.height * (zoom - 1f)) / 2f
    return Offset(x = pan.x.coerceIn(-maxX, maxX), y = pan.y.coerceIn(-maxY, maxY))
}
