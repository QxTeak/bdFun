package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.common.biliImageThumbUrl
import kotlin.math.abs

/**
 * 大图单页：从缩略图的裁切画面展开成完整图片，并支持双指缩放、双击放大、放大后平移。
 *
 * 关键点：每一帧同时插值「裁剪窗口」和「图片倍率」。起点窗口就是缩略图那个矩形、
 * 倍率是刚好铺满它的裁切倍率，所以第一帧和列表里的缩略图完全重合；终点倍率为 1。
 *
 * 性能：入场进度/拖动位移只在 [graphicsLayer] 和 [drawWithContent] 里读取，
 * 组合阶段只依赖「宽高比」这种一次性的值。这样 250ms 的动画每帧只失效绘制层，
 * 不会把两张 AsyncImage、手势层和整棵页面反复重组（Telegram 也是这个思路）。
 */
@Composable
internal fun PhotoViewerPage(
    url: String,
    request: PhotoViewerRequest,
    view: PhotoViewerTransitionState,
    /** 只有从缩略图打开的那一页才有起点矩形；翻到别的图片后不再借用它的位置。 */
    origin: Rect?,
    originRadiusPx: Float,
    onDismiss: () -> Unit,
    /** 长按图片：弹出保存菜单，位置用长按点（窗口坐标）。 */
    onLongPress: (String, Offset) -> Unit = { _, _ -> },
) {
    val page = remember(url) {
        PhotoViewerPageState().also {
            val aspect = view.openAspect
            if (aspect > 0.01f) it.aspect = aspect
        }
    }
    LaunchedEffect(page.zoom) { view.zoom = page.zoom }

    // 原图只在场动画结束后才开始组合/解码（Telegram 也是在 animationEnd 后才 setImages）。
    // 用阈值状态而不是每帧读进度，跨过 0.995 时只重组一次。
    val showFull by remember(url) {
        androidx.compose.runtime.derivedStateOf { view.progress.value >= 0.995f }
    }
    var fullReady by remember(url) { mutableStateOf(false) }
    val fullAlpha by animateFloatAsState(
        targetValue = if (fullReady && showFull) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "photo-full-fade",
    )

    val density = LocalDensity.current
    val screenWidth = view.size.width.toFloat()
    val screenHeight = view.size.height.toFloat()
    if (screenWidth <= 0f || screenHeight <= 0f) return

    val aspect = if (page.aspect > 0.01f) page.aspect else screenWidth / screenHeight
    // Contain 尺寸只随宽高比变化，是组合期常量；动画帧不再重新测量布局。
    val imageWidthPx = if (screenWidth / screenHeight > aspect) screenHeight * aspect else screenWidth
    val imageHeightPx = imageWidthPx / aspect
    val spec = PhotoStageSpec(
        aspect = aspect,
        imageWidth = with(density) { imageWidthPx.toDp() },
        imageHeight = with(density) { imageHeightPx.toDp() },
        thumbModel = remember(url) { biliImageThumbUrl(url, PhotoThumbWidth) },
        fullAlpha = fullAlpha,
        showFull = showFull,
        origin = origin,
        originRadiusPx = originRadiusPx,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
    )
    PhotoViewerStage(url, page, view, spec, onDismiss, onLongPress) { width, height ->
        if (width > 0 && height > 0) page.aspect = width.toFloat() / height
        fullReady = true
    }
    // 等原图真正淡入到接近不透明再撤盖层；只等“加载完成”会在全图还黑着时提前露出。
    LaunchedEffect(fullAlpha) {
        if (fullAlpha > 0.99f) view.markCoverReady()
    }
}

/** 一帧内所有会变的几何/透明度，打包传给各层，避免函数参数超过 12 个。 */
private data class PhotoStageSpec(
    val aspect: Float,
    val imageWidth: Dp,
    val imageHeight: Dp,
    val thumbModel: String,
    val fullAlpha: Float,
    val showFull: Boolean,
    val origin: Rect?,
    val originRadiusPx: Float,
    val screenWidth: Float,
    val screenHeight: Float,
) {
    fun morphAt(progress: Float): PhotoMorphFrame = photoMorphFrame(
        progress = progress,
        originX = origin?.left ?: 0f,
        originY = origin?.top ?: 0f,
        originWidth = origin?.width ?: 0f,
        originHeight = origin?.height ?: 0f,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        imageAspect = aspect,
        originRadius = originRadiusPx,
    )
}

@Composable
private fun PhotoViewerStage(
    url: String,
    page: PhotoViewerPageState,
    view: PhotoViewerTransitionState,
    spec: PhotoStageSpec,
    onDismiss: () -> Unit,
    onLongPress: (String, Offset) -> Unit,
    onFullLoaded: (Int, Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 手势的 key 只跟页面地址绑定：之前用每帧都在变的裁剪框当 key，
            // 入场动画期间会逐帧销毁重建手势识别器，白掉帧。
            .pointerInput(url) {
                detectTapGestures(
                    // 点画面任意位置都是关闭（不再要求必须点到图片外的空白）。
                    onTap = { onDismiss() },
                    onDoubleTap = { tap -> page.zoom = toggleZoom(page.zoom, tap, view.size) },
                    onLongPress = { tap -> onLongPress(url, tap) },
                )
            }
            .photoZoomGestures(
                onZoom = { page.zoom = it },
                onPan = { page.pan = it },
                currentZoom = { page.zoom },
                currentPan = { page.pan },
                size = { view.size },
            )
            .graphicsLayer {
                // 拖动位移随进度衰减：进度归零时正好落在缩略图上，不会带着偏移落地再跳回去。
                translationY = view.dragY * view.progress.value
            },
    ) {
        Box(Modifier.fillMaxSize().photoMorphClip(spec, view)) {
            PhotoViewerLayers(url, page, view, spec, onFullLoaded)
        }
    }
}

@Composable
private fun PhotoViewerLayers(
    url: String,
    page: PhotoViewerPageState,
    view: PhotoViewerTransitionState,
    spec: PhotoStageSpec,
    onFullLoaded: (Int, Int) -> Unit,
) {
    // 原图还没就绪时只画缩略图：它和卡片是同一张源图、同样的中心裁切，
    // 所以入场第一帧、退场最后一帧都和列表里的缩略图完全重合。
    if (!spec.showFull || spec.fullAlpha < 0.999f) {
        PhotoMorphImage(
            model = spec.thumbModel,
            spec = spec,
            view = view,
            zoom = { page.zoom },
            pan = { page.pan },
            // Crop：宽高比还没算出来时也保证铺满框，不会出现黑边。
            contentScale = ContentScale.Crop,
            // 缩略图也报一次宽高比：它命中缓存后立刻就能开始入场动画。
            onLoaded = { width, height ->
                if (width > 0 && height > 0) page.aspect = width.toFloat() / height
            },
        )
    }
    if (spec.showFull) {
        PhotoMorphImage(
            model = url,
            spec = spec,
            view = view,
            zoom = { page.zoom },
            pan = { page.pan },
            alpha = spec.fullAlpha,
            onLoaded = onFullLoaded,
        )
    }
}

/** 和列表一致的缩略图宽度；宽度决定裁切尺寸，必须和 FeedImageGrid 保持同步。 */
private const val PhotoThumbWidth = 720

/**
 * 图片按完整尺寸布局；每帧的缩放/中心位移都在 [graphicsLayer] 里算。
 * 布局只依赖屏幕宽高比，动画期间不触发测量和重组。
 */
@Composable
private fun PhotoMorphImage(
    model: Any?,
    spec: PhotoStageSpec,
    view: PhotoViewerTransitionState,
    zoom: () -> Float,
    pan: () -> Offset,
    alpha: Float = 1f,
    contentScale: ContentScale = ContentScale.Fit,
    onLoaded: ((Int, Int) -> Unit)? = null,
) {
    AsyncImage(
        model = rememberImageRequest(model, crossfade = false),
        contentDescription = null,
        contentScale = contentScale,
        onSuccess = { state ->
            // 用 drawable 的固有尺寸而不是 painter.intrinsicSize：Coil 2.x 里前者一定有，
            // 后者在部分请求类型下会是 Unspecified。
            val drawable = state.result.drawable
            onLoaded?.invoke(drawable.intrinsicWidth, drawable.intrinsicHeight)
        },
        modifier = Modifier
            .size(spec.imageWidth, spec.imageHeight)
            .graphicsLayer {
                val morph = spec.morphAt(view.progress.value)
                val scale = morph.imageScale * zoom()
                scaleX = scale
                scaleY = scale
                // 图片布局在 (0,0)，把它平移到当前帧的中心。
                translationX = morph.imageCenterX - size.width / 2f + pan().x
                translationY = morph.imageCenterY - size.height / 2f + pan().y
                this.alpha = alpha
            },
    )
}

/** 只在绘制阶段裁剪，图片布局尺寸不受影响；进度也在绘制阶段读，避免逐帧重组。 */
private fun Modifier.photoMorphClip(
    spec: PhotoStageSpec,
    view: PhotoViewerTransitionState,
): Modifier = drawWithContent {
    val progress = view.progress.value
    // 完全展开后不再裁剪：否则放大后的图片会被自身矩形（比屏幕小）切掉。
    if (progress >= 0.999f) {
        drawContent()
        return@drawWithContent
    }
    val morph = spec.morphAt(progress)
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
