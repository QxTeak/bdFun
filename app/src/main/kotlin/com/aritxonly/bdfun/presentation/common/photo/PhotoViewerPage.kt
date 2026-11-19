package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.common.biliImageThumbUrl
import kotlin.math.abs

/**
 * 大图单页：从缩略图的裁切画面展开成完整图片，并支持双指缩放、双击放大、放大后平移。
 *
 * 关键点：每一帧同时插值「裁剪窗口」和「图片倍率」。起点窗口就是缩略图那个矩形、
 * 倍率是刚好铺满它的裁切倍率，所以第一帧和列表里的缩略图完全重合；终点倍率为 1。
 * 注意裁剪窗口不能用 size 去约束图片——那样图片会被父约束压小，再乘裁切倍率就成了一个点。
 * 这里图片始终按完整尺寸布局，只在绘制阶段用路径裁剪。
 *
 * 两层图：底层是列表里同一张缩略图（只缩宽度、不裁切，缓存命中，入场/退场第一帧就贴合），
 * 上层是原图，加载完成后淡入。
 *
 * 性能：入场进度只在绘制阶段读取（graphicsLayer / drawWithContent），组合期不再逐帧重组
 * 两层图片；裁剪 Path 只在 drawWithCache 里创建一次并复用。
 */
@Composable
internal fun PhotoViewerPage(
    url: String,
    request: PhotoViewerRequest,
    view: PhotoViewerTransitionState,
    /** 只有从缩略图打开的那一页才有起点矩形；翻到别的图片后不再借用它的位置。 */
    origin: androidx.compose.ui.geometry.Rect?,
    originRadiusPx: Float,
    onDismiss: () -> Unit,
    /** 长按图片：弹出保存菜单，位置用长按点（窗口坐标）。 */
    onLongPress: (String, Offset) -> Unit = { _, _ -> },
) {
    val page = remember(url) { PhotoViewerPageState() }
    LaunchedEffect(page.zoom) { view.zoom = page.zoom }

    // 原图就绪后淡入，避免入场动画期间"空一下再跳出来"。
    // 用 Animatable 而不是 animateFloatAsState：透明度改到 graphicsLayer 里读取，不逐帧重组。
    var fullReady by remember(url) { mutableStateOf(false) }
    val fullAlpha = remember(url) { Animatable(0f) }
    LaunchedEffect(fullReady) { fullAlpha.animateTo(if (fullReady) 1f else 0f, tween(160)) }

    val density = LocalDensity.current
    val screenWidth = view.size.width.toFloat()
    val screenHeight = view.size.height.toFloat()
    if (screenWidth <= 0f || screenHeight <= 0f) return

    val imageAspect = page.aspect.takeIf { it > 0.01f } ?: (screenWidth / screenHeight)
    val imageWidthPx = if (screenWidth / screenHeight > imageAspect) screenHeight * imageAspect else screenWidth
    val imageHeightPx = imageWidthPx / imageAspect
    val imageWidthDp = with(density) { imageWidthPx.toDp() }
    val imageHeightDp = with(density) { imageHeightPx.toDp() }

    val originX = origin?.left ?: 0f
    val originY = origin?.top ?: 0f
    val originWidth = origin?.width ?: 0f
    val originHeight = origin?.height ?: 0f
    // 只捕获不随动画变化的量；progress 在 lambda 执行（绘制阶段）时才读取。
    val morphAt: () -> PhotoMorphFrame = remember(
        originX, originY, originWidth, originHeight, screenWidth, screenHeight, imageAspect, originRadiusPx,
    ) {
        {
            photoMorphFrame(
                progress = view.progress.value,
                originX = originX,
                originY = originY,
                originWidth = originWidth,
                originHeight = originHeight,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                imageAspect = imageAspect,
                originRadius = originRadiusPx,
            )
        }
    }
    val stage = PhotoStageFrame(
        morph = morphAt,
        thumbModel = remember(url) { biliImageThumbUrl(url, PhotoThumbWidth) },
        fullAlpha = { fullAlpha.value },
        inTransition = { view.progress.value < 0.999f },
        imageWidth = imageWidthDp,
        imageHeight = imageHeightDp,
        // 缩放/平移也跟着进度收敛：放大过再退出时，画面会平滑回到缩略图而不是"啪"一下跳回去。
        zoom = { 1f + (page.zoom - 1f) * view.progress.value },
        pan = { page.pan * view.progress.value },
        // 拖动位移随进度衰减：进度归零时正好落在缩略图上，不会带着偏移落地再跳回去。
        dragY = { view.dragY * view.progress.value },
        onAspect = { page.aspect = it },
    )
    PhotoViewerStage(url, page, view, stage, onDismiss, onLongPress) { width, height ->
        if (width > 0 && height > 0) page.aspect = width.toFloat() / height
        fullReady = true
        // 告诉宿主：宽高比已经确定，可以放心跑入场动画了。
        view.contentReady = true
    }
}

/** 一帧内所有会变的几何/透明度，全部用 lambda 晚读，避免逐帧重组。 */
private class PhotoStageFrame(
    val morph: () -> PhotoMorphFrame,
    val thumbModel: String,
    val fullAlpha: () -> Float,
    val inTransition: () -> Boolean,
    val imageWidth: Dp,
    val imageHeight: Dp,
    val zoom: () -> Float,
    val pan: () -> Offset,
    val dragY: () -> Float,
    val onAspect: (Float) -> Unit,
)

@Composable
private fun PhotoViewerStage(
    url: String,
    page: PhotoViewerPageState,
    view: PhotoViewerTransitionState,
    stage: PhotoStageFrame,
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
            .graphicsLayer { translationY = stage.dragY() },
    ) {
        Box(Modifier.fillMaxSize().morphClip(stage.morph, stage.inTransition)) {
            PhotoViewerLayers(url, view, stage, onFullLoaded)
        }
    }
}

@Composable
private fun PhotoViewerLayers(
    url: String,
    view: PhotoViewerTransitionState,
    stage: PhotoStageFrame,
    onFullLoaded: (Int, Int) -> Unit,
) {
    // 原图完全淡入后才移除缩略图；这个判断只在跨过阈值时重组一次。
    val thumbVisible by remember { derivedStateOf { stage.fullAlpha() < 0.999f } }
    // 原图还没就绪时只画缩略图：它和卡片是同一张源图、同样的中心裁切，
    // 所以入场第一帧、退场最后一帧都和列表里的缩略图完全重合。
    if (thumbVisible) {
        MorphImage(
            model = stage.thumbModel,
            morph = stage.morph,
            width = stage.imageWidth,
            height = stage.imageHeight,
            zoom = stage.zoom,
            pan = stage.pan,
            // Crop：宽高比还没算出来时也保证铺满框，不会出现黑边。
            contentScale = ContentScale.Crop,
            // 缩略图也报一次宽高比：它命中缓存后立刻就能开始入场动画。
            onLoaded = { width, height ->
                if (width > 0 && height > 0) stage.onAspect(width.toFloat() / height)
                view.contentReady = true
            },
        )
    }
    MorphImage(
        model = url,
        morph = stage.morph,
        width = stage.imageWidth,
        height = stage.imageHeight,
        zoom = stage.zoom,
        pan = stage.pan,
        alpha = stage.fullAlpha,
        onLoaded = onFullLoaded,
    )
}

/** 和列表一致的缩略图宽度；宽度决定裁切尺寸，必须和 FeedImageGrid 保持同步。 */
private const val PhotoThumbWidth = 720

/** 图片按完整尺寸布局，用 graphicsLayer 定位，布局约束永远不参与裁剪。 */
@Composable
private fun MorphImage(
    model: Any?,
    morph: () -> PhotoMorphFrame,
    width: Dp,
    height: Dp,
    zoom: () -> Float,
    pan: () -> Offset,
    alpha: () -> Float = { 1f },
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
            .size(width, height)
            .graphicsLayer {
                val frame = morph()
                val scale = frame.imageScale * zoom()
                scaleX = scale
                scaleY = scale
                translationX = frame.imageCenterX - size.width / 2f + pan().x
                translationY = frame.imageCenterY - size.height / 2f + pan().y
                this.alpha = alpha()
            },
    )
}

/** 只在绘制阶段裁剪，图片布局尺寸不受影响；Path 在缓存块里复用。 */
private fun Modifier.morphClip(
    morph: () -> PhotoMorphFrame,
    skipClip: () -> Boolean,
): Modifier = drawWithCache {
    val path = Path()
    onDrawWithContent {
        if (skipClip()) {
            drawContent()
            return@onDrawWithContent
        }
        val frame = morph()
        path.reset()
        path.addRoundRect(
            RoundRect(
                left = frame.clipLeft,
                top = frame.clipTop,
                right = frame.clipLeft + frame.clipWidth,
                bottom = frame.clipTop + frame.clipHeight,
                radiusX = frame.cornerRadius,
                radiusY = frame.cornerRadius,
            ),
        )
        clipPath(path) { this@onDrawWithContent.drawContent() }
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
