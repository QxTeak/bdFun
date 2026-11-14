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
import androidx.compose.foundation.layout.offset
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
import com.aritxonly.bdfun.core.common.biliImageThumbUrl
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 大图单页：从缩略图的裁切画面展开成完整图片，并支持双指缩放、双击放大、放大后平移。
 *
 * 关键点：每一帧同时插值「裁剪窗口」和「图片倍率」。起点窗口就是缩略图那个矩形、
 * 倍率是刚好铺满它的裁切倍率，所以第一帧和列表里的缩略图完全重合；终点倍率为 1。
 * 注意裁剪窗口不能用 size 去约束图片——那样图片会被父约束压小，再乘裁切倍率就成了一个点。
 * 这里图片始终按完整尺寸布局，只在绘制阶段用路径裁剪。
 *
 * 两层图：底层是列表里同一张 16:9 裁切缩略图（缓存命中，入场/退场第一帧就贴合），
 * 上层是原图，加载完成后淡入。这样既不会出现"切到原图后闪一下"，也能保证放大后是完整图。
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
    var fullReady by remember(url) { mutableStateOf(false) }
    val fullAlpha by animateFloatAsState(
        targetValue = if (fullReady) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "photo-full-fade",
    )

    val density = LocalDensity.current
    val screenWidth = view.size.width.toFloat()
    val screenHeight = view.size.height.toFloat()
    if (screenWidth <= 0f || screenHeight <= 0f) return

    val morph = photoMorphFrame(
        progress = view.progress.value,
        originX = origin?.left ?: 0f,
        originY = origin?.top ?: 0f,
        originWidth = origin?.width ?: 0f,
        originHeight = origin?.height ?: 0f,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        imageAspect = page.aspect,
        originRadius = originRadiusPx,
    )
    // 拖动位移随进度衰减：进度归零时正好落在缩略图上，不会带着偏移落地再跳回去。
    val dragY = view.dragY * view.progress.value
    // 缩放/平移也跟着进度收敛：放大过再退出时，画面会平滑回到缩略图而不是"啪"一下跳回去。
    val settle = view.progress.value
    val zoomNow = 1f + (page.zoom - 1f) * settle
    val panNow = page.pan * settle
    // 完全展开后不再裁剪：否则放大后的图片会被自身矩形（比屏幕小）切掉。
    val inTransition = view.progress.value < 0.999f
    val imageWidthDp = with(density) { morph.imageWidth.toDp() }
    val imageHeightDp = with(density) { morph.imageHeight.toDp() }
    // 和列表缩略图完全同一张（同样只缩宽度、不裁切），Coil 直接命中缓存。
    val thumbModel = remember(url) { biliImageThumbUrl(url, PhotoThumbWidth) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(morph.clipLeft, morph.clipTop, morph.clipWidth, morph.clipHeight) {
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
            .graphicsLayer { translationY = dragY },
    ) {
        Box(Modifier.fillMaxSize().morphClip(morph, skipClip = !inTransition)) {
            // 原图还没就绪时只画缩略图：它和卡片是同一张源图、同样的中心裁切，
            // 所以入场第一帧、退场最后一帧都和列表里的缩略图完全重合。
            if (fullAlpha < 0.999f) {
                MorphImage(
                    model = thumbModel,
                    morph = morph,
                    width = imageWidthDp,
                    height = imageHeightDp,
                    zoom = zoomNow,
                    pan = panNow,
                    // Crop：宽高比还没算出来时也保证铺满框，不会出现黑边。
                    contentScale = ContentScale.Crop,
                    // 缩略图也报一次宽高比：它命中缓存后立刻就能开始入场动画。
                    onLoaded = { width, height ->
                        if (width > 0 && height > 0) page.aspect = width.toFloat() / height
                        view.contentReady = true
                    },
                )
            }
            MorphImage(
                model = url,
                morph = morph,
                width = imageWidthDp,
                height = imageHeightDp,
                zoom = zoomNow,
                pan = panNow,
                alpha = fullAlpha,
                onLoaded = { width, height ->
                    if (width > 0 && height > 0) page.aspect = width.toFloat() / height
                    fullReady = true
                    // 告诉宿主：宽高比已经确定，可以放心跑入场动画了。
                    view.contentReady = true
                },
            )
        }
    }
}

/** 和列表一致的缩略图宽度；宽度决定裁切尺寸，必须和 FeedImageGrid 保持同步。 */
private const val PhotoThumbWidth = 720

/** 图片按完整尺寸布局，用 offset 定位中心、graphicsLayer 做裁切倍率，布局约束永远不参与裁剪。 */
@Composable
private fun MorphImage(
    model: Any?,
    morph: PhotoMorphFrame,
    width: Dp,
    height: Dp,
    zoom: Float,
    pan: Offset,
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
                this.alpha = alpha
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
