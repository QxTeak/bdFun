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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 大图单页：从缩略图的裁切画面展开成完整图片，并支持双指缩放、双击放大、放大后平移。
 *
 * 关键点：每一帧同时插值「裁剪窗口」和「图片倍率」。起点窗口就是缩略图那个矩形、
 * 倍率是刚好铺满它的裁切倍率，所以第一帧和列表里的缩略图完全重合；终点倍率为 1。
 * 注意裁剪窗口不能用 size 去约束图片——那样图片会被父约束压小，再乘裁切倍率就成了一个点。
 * 这里图片始终按完整尺寸布局，只在绘制阶段用路径裁剪。
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
    // 完全展开后不再裁剪：否则放大后的图片会被自身矩形（比屏幕小）切掉。
    val inTransition = view.progress.value < 0.999f
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
        Box(Modifier.fillMaxSize().morphClip(morph, skipClip = !inTransition)) {
            MorphImage(
                url = url,
                morph = morph,
                width = imageWidthDp,
                height = imageHeightDp,
                zoom = zoom,
                pan = pan,
                onAspect = { aspect = it },
            )
        }
    }
}

/** 图片按完整尺寸布局，用 offset 定位中心、graphicsLayer 做裁切倍率，布局约束永远不参与裁剪。 */
@Composable
private fun MorphImage(
    url: String,
    morph: PhotoMorphFrame,
    width: Dp,
    height: Dp,
    zoom: Float,
    pan: Offset,
    onAspect: (Float) -> Unit,
) {
    AsyncImage(
        model = rememberImageRequest(url, crossfade = false),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        onSuccess = { state ->
            // 用 drawable 的固有尺寸而不是 painter.intrinsicSize：Coil 2.x 里前者一定有，
            // 后者在部分请求类型下会是 Unspecified。
            val drawable = state.result.drawable
            val imageWidth = drawable.intrinsicWidth
            val imageHeight = drawable.intrinsicHeight
            if (imageWidth > 0 && imageHeight > 0) onAspect(imageWidth.toFloat() / imageHeight)
        },
        modifier = Modifier
            .offset {
                IntOffset(
                    (morph.imageCenterX - morph.imageWidth / 2f).roundToInt(),
                    (morph.imageCenterY - morph.imageHeight / 2f).roundToInt(),
                )
            }
            .size(width, height)
            .graphicsLayer {
                val scale = morph.imageScale * zoom
                scaleX = scale
                scaleY = scale
                translationX = pan.x
                translationY = pan.y
            },
    )
}

/**
 * 只在绘制阶段裁剪，图片布局尺寸不受影响。
 */
private fun Modifier.morphClip(morph: PhotoMorphFrame, skipClip: Boolean): Modifier = drawWithContent {
    if (skipClip) {
        drawContent()
        return@drawWithContent
    }
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = morph.clipLeft,
                top = morph.clipTop,
                right = morph.clipLeft + morph.clipWidth,
                bottom = morph.clipTop + morph.clipHeight,
                radiusX = morph.cornerRadius,
                radiusY = morph.cornerRadius,
            ),
        )
    }
    clipPath(path) { this@drawWithContent.drawContent() }
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
