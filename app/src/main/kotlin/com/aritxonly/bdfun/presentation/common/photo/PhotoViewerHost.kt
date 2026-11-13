package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 同窗口的大图查看层。
 *
 * 为什么不用 Dialog：Dialog 是另一个 Window，背后的页面根本不在同一棵渲染树里，
 * 既做不出「拖动时背后页面缩到 0.9」，也没法和缩略图共享元素。
 *
 * 结构上它是宿主：内容被包在下面一层，大图作为覆盖层画在上面，
 * 两层共享同一个动画进度，所以入场/跟手/退场是同一条时间线。
 */
@Composable
internal fun PhotoViewerHost(
    state: PhotoViewerStateHolder,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val incoming = state.request
    var displayed by remember { mutableStateOf<PhotoViewerRequest?>(null) }
    val progress = remember { Animatable(0f) }
    // 跟手阶段用普通状态逐帧更新，只有在回弹时才跑动画；
    // 每个事件都启一个协程去 snapTo 会把主线程压垮。
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var pageZoom by remember { mutableFloatStateOf(1f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val animations = com.aritxonly.bdfun.AppGraph.settings.animationsEnabled
    val spec = tween<Float>(
        durationMillis = if (animations) 250 else 0,
        easing = CubicBezierEasing(0f, 0f, 0.2f, 1f),
    )

    LaunchedEffect(incoming, animations) {
        if (incoming != null) {
            displayed = incoming
            progress.snapTo(0f)
            dragOffset = Offset.Zero
            pageZoom = 1f
            progress.animateTo(1f, spec)
        } else if (displayed != null) {
            launch { animate(dragOffset.y, 0f, animationSpec = spec) { value, _ -> dragOffset = Offset(0f, value) } }
            progress.animateTo(0f, spec)
            displayed = null
            pageZoom = 1f
        }
    }

    val dismiss: () -> Unit = { state.close() }
    // 大图打开时返回键先关大图，不要直接把页面退掉。
    androidx.activity.compose.BackHandler(enabled = displayed != null) { state.close() }
    val dismissProgress = photoDismissProgress(dragOffset.y, size.height.toFloat())

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(progress.value, pageZoom) {
                if (progress.value < 0.99f || pageZoom > 1.01f) return@pointerInput
                var lastTime = 0L
                var totalY = 0f
                detectVerticalDragGestures(
                    onDragStart = { lastTime = System.currentTimeMillis(); totalY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalY += dragAmount
                        dragOffset = Offset(0f, totalY)
                    },
                    onDragEnd = {
                        val elapsed = (System.currentTimeMillis() - lastTime).coerceAtLeast(1L)
                        val velocity = totalY / elapsed * 1000f
                        val p = photoDismissProgress(totalY, size.height.toFloat())
                        if (photoShouldDismiss(p, velocity)) {
                            dismiss()
                        } else {
                            scope.launch {
                                animate(dragOffset.y, 0f, animationSpec = spring()) { value, _ -> dragOffset = Offset(0f, value) }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            animate(dragOffset.y, 0f, animationSpec = spring()) { value, _ -> dragOffset = Offset(0f, value) }
                        }
                    },
                )
            },
    ) {
        // 底层页面：跟手时缩到 0.9，松手回弹。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = photoBackdropScale(if (displayed != null) dismissProgress else 0f)
                    scaleX = s
                    scaleY = s
                },
        ) {
            content()
        }

        val request = displayed
        if (request != null) {
            PhotoViewerOverlay(
                request = request,
                progress = progress.value,
                dragOffset = dragOffset,
                dismissProgress = dismissProgress,
                widthPx = size.width.toFloat(),
                heightPx = size.height.toFloat(),
                onZoomChanged = { pageZoom = it },
                onDismiss = dismiss,
            )
        }
    }
}

@Composable
private fun PhotoViewerOverlay(
    request: PhotoViewerRequest,
    progress: Float,
    dragOffset: Offset,
    dismissProgress: Float,
    widthPx: Float,
    heightPx: Float,
    onZoomChanged: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val width = widthPx
    val height = heightPx
    val origin = request.origin
    val geometry = photoGeometryAt(
        progress = progress,
        screenWidth = width,
        screenHeight = height,
        originX = origin?.left ?: (width / 2f - width * 0.45f),
        originY = origin?.top ?: (height / 2f - height * 0.45f),
        originWidth = origin?.width ?: (width * 0.9f),
        originHeight = origin?.height ?: (height * 0.9f),
        originRadius = request.originRadiusPx,
    )
    val backgroundAlpha = (progress * (1f - dismissProgress)).coerceIn(0f, 1f)
    val chromeAlpha = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
    val cornerDp = with(density) { geometry.cornerRadius.toDp() }

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = geometry.scale * photoDragScale(dismissProgress),
                    scaleY = geometry.scale * photoDragScale(dismissProgress),
                    translationX = geometry.translationX + dragOffset.x,
                    translationY = geometry.translationY + dragOffset.y,
                    shape = RoundedCornerShape(cornerDp),
                    clip = true,
                ),
        ) {
            key(request) {
                PhotoViewerPager(request, onZoomChanged)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = chromeAlpha },
        ) {
            Box(Modifier.align(Alignment.TopCenter)) {
                DynamicViewerTopBar(onDismiss)
            }
            request.footer?.let { footer ->
                Box(Modifier.align(Alignment.BottomCenter)) {
                    footer(onDismiss)
                }
            }
        }
    }
}

@Composable
private fun PhotoViewerPager(request: PhotoViewerRequest, onZoomChanged: (Float) -> Unit) {
    val pager = rememberPagerState(initialPage = request.index, pageCount = { request.images.size })
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        PhotoViewerPage(
            url = request.images[page],
            onScaleChanged = onZoomChanged,
        )
    }
}
