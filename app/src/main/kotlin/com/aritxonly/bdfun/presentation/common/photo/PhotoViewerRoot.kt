package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Bitmap
import android.graphics.Rect
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 当前窗口内的图片查看器宿主。
 *
 * 和 Telegram 的 PhotoViewer 一样直接画在窗口最上层，不创建 Dialog；入场/退场用
 * 列表里已经缓存的缩略图 bitmap 跑一次 [PhotoMorphView]，期间不组合 pager、不解码原图。
 * 动画结束后才显示可交互的 Compose 查看器并加载原图。
 *
 * 楼中楼这类独立 Dialog 会在自己的内容里再包一层本宿主，查看器画在弹窗自己的窗口里。
 */
@Composable
internal fun PhotoViewerRoot(
    animationsEnabled: Boolean,
    state: PhotoViewerStateHolder = remember { PhotoViewerStateHolder() },
    content: @Composable () -> Unit,
) {
    val view = remember { PhotoViewerTransitionState() }
    val openDurationMs = if (animationsEnabled) PhotoViewerMotion.OpenDurationMs else 0L
    val closeDurationMs = if (animationsEnabled) PhotoViewerMotion.CloseDurationMs else 0L
    val viewerBackdrop = rememberLayerBackdrop {
        drawRect(androidx.compose.ui.graphics.Color.Black)
        drawContent()
    }

    // 查看器状态只在 Host 里读取：打开大图时只重组 Host，不带着整页 content() 一起重组。
    // 这是首次打开卡顿里最容易被忽略的一块：页面越大，宿主重组越贵。
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { view.size = it },
    ) {
        // 底层页面：打开时随进度放大一点点，关闭时按同样倍数缩回；
        // 跟手拖动时再缩到 0.9。
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val scale = photoBackdropScaleAt(view.progress.value, view.dismissProgress)
                scaleX = scale
                scaleY = scale
            },
        ) {
            CompositionLocalProvider(LocalPhotoViewer provides state) { content() }
        }
        PhotoViewerHost(state, view, openDurationMs, closeDurationMs, viewerBackdrop)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.PhotoViewerHost(
    state: PhotoViewerStateHolder,
    view: PhotoViewerTransitionState,
    openDurationMs: Long,
    closeDurationMs: Long,
    viewerBackdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop,
) {
    val context = LocalContext.current
    // 提前创建形变 View：首次打开时只改尺寸和启动动画，不再现场 inflate/类加载，
    // 这是首次打开卡顿里最确定的一笔。空闲时保持 1dp 且不可见。
    val morphView = remember(context) {
        PhotoMorphView(context).apply {
            onProgress = { view.progress.floatValue = it }
            onFinished = {
                if (state.request == null) {
                    view.displayed = null
                    view.progress.floatValue = 0f
                    view.morphParams = null
                    view.morphCovering = false
                    view.resetTransient()
                } else {
                    view.settled = true
                    view.progress.floatValue = 1f
                }
            }
        }
    }
    val incoming = state.request
    LaunchedEffect(incoming) {
        val request = incoming
        val current = view.displayed
        when {
            request != null && current == null -> {
                // 上一次拖动关闭可能留下位移/缩放；不重置会让再次打开时位置和背景都不对。
                view.resetTransient()
                view.displayed = request
                view.pageIndex = request.index
                view.settled = false
                view.progress.floatValue = 0f
                view.startMorph(request, request.index, open = true, openDurationMs)
            }
            request == null && current != null -> {
                view.settled = false
                // 查看器退出时菜单不能继续挂在自己的录制层上：先撤掉菜单与背板录制，
                // 再走形变回落，否则会看到一帧没有采样源的玻璃面板（闪一下）。
                view.menuVisible = false
                view.startMorph(current, view.pageIndex, open = false, closeDurationMs)
            }
        }
    }
    // 原图/缩略图迟迟没画出来、或 Compose 层已能画了：统一由这个 effect 处理盖层淡出。
    PhotoMorphCoverFallback(view) { morphView }
    // 入场动画/缩略图盖层期间点击也要能立即关闭，不能等原图加载完。
    val dismissOnTap = view.displayed != null && (!view.settled || view.morphCovering)
    if (dismissOnTap) {
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { state.close() }
            },
        )
    }
    view.displayed?.let { request ->
        if (view.settled) {
            PhotoViewerSettledLayer(request, view, state, viewerBackdrop)
        }
    }
    val morphActive = view.displayed != null && view.morphParams != null &&
        (!view.settled || view.morphCovering)
    BindMorphAnimation(view, morphView)
    AndroidView(
        factory = { morphView },
        modifier = if (morphActive) {
            Modifier.fillMaxSize()
        } else {
            Modifier.size(1.dp).graphicsLayer { alpha = 0f }
        },
    )
}

/** session 变化后启动；start() 里会等到 View 真正铺满窗口再开始，避免在 1dp 预热点上跑动画。 */
@Composable
private fun BindMorphAnimation(
    view: PhotoViewerTransitionState,
    morphView: PhotoMorphView,
) {
    LaunchedEffect(view.morphSession) {
        val params = view.morphParams ?: return@LaunchedEffect
        morphView.start(
            params.bitmap,
            params.origin,
            params.radius,
            params.open,
            durationMs = params.duration,
            dragOffsetY = params.dragOffsetY,
            startDim = params.startDim,
        )
    }
}

/** 缩略图盖层的两种淡出时机：等太久兜底，或 Compose 层报告已经能画。 */
@Composable
private fun PhotoMorphCoverFallback(
    view: PhotoViewerTransitionState,
    overlay: () -> PhotoMorphView?,
) {
    // 原图/缩略图迟迟没画出来时，缩略图不能永远盖着；超时后露出 Compose 层。
    LaunchedEffect(view.settled, view.morphCovering) {
        if (view.settled && view.morphCovering) {
            kotlinx.coroutines.delay(600)
            if (view.morphCovering) {
                val target = overlay()
                if (target != null) target.fadeOut { view.morphCovering = false } else view.morphCovering = false
            }
        }
    }
    // Compose 层说图已经能画了：等它真正画出几帧后再淡出盖层，杜绝中间闪一帧空。
    PhotoCoverReadyEffect(view, overlay)
}

@Composable
private fun PhotoCoverReadyEffect(
    view: PhotoViewerTransitionState,
    overlay: () -> PhotoMorphView?,
) {
    LaunchedEffect(view.settled, view.coverReadyToken) {
        if (view.settled && view.morphCovering && view.coverReadyToken > 0) {
            kotlinx.coroutines.delay(120)
            if (!view.morphCovering) return@LaunchedEffect
            val target = overlay()
            if (target != null) {
                target.fadeOut { view.morphCovering = false }
            } else {
                view.morphCovering = false
            }
        }
    }
}

@Composable
private fun PhotoViewerSettledLayer(
    request: PhotoViewerRequest,
    view: PhotoViewerTransitionState,
    state: PhotoViewerStateHolder,
    viewerBackdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop,
) {
    // 和 Telegram 一样画在同一个窗口里；楼中楼有自己的宿主层。
    CompositionLocalProvider(LocalTeakBackdrop provides viewerBackdrop) {
        Box(
            Modifier.fillMaxSize().then(
                if (view.menuVisible) Modifier.layerBackdrop(viewerBackdrop) else Modifier,
            ),
        ) {
            PhotoViewerOverlay(
                request = request,
                view = view,
                onDismiss = state::close,
                onPageChanged = {
                    view.pageIndex = it
                    // 换一张图从「未旋转」开始，避免上一张的旋转带到下一张。
                    view.rotationQuarter = 0
                },
            )
        }
    }
}

/** 与 [PhotoViewerMotion] 保持一致，具体时长由打开/关闭方向分别决定。 */


/**
 * 一次打开/关闭过程中的全部瞬时状态。
 *
 * 入场/退场由 [PhotoMorphView] 的单个 ValueAnimator 驱动；progress 只是给底层页面
 * 缩放和 Compose 层读取的镜像，不再用 Compose 动画逐帧重组查看器。
 */
internal class PhotoViewerTransitionState {
    var displayed by mutableStateOf<PhotoViewerRequest?>(null)
    var settled by mutableStateOf(false)
    var pageIndex by mutableIntStateOf(0)
    val progress = mutableFloatStateOf(0f)
    var dragY by mutableFloatStateOf(0f)
    var zoom by mutableFloatStateOf(1f)
    var chromeVisible by mutableStateOf(true)
    var size by mutableStateOf(IntSize.Zero)
    var menuVisible by mutableStateOf(false)
    /** 用户手动旋转的第几个 90°：图片查看器不再跟随传感器转向，只认这个值。 */
    var rotationQuarter by mutableIntStateOf(0)
    var morphParams by mutableStateOf<PhotoMorphParams?>(null)
    var morphSession by mutableIntStateOf(0)
    /** 缩略图继续盖在查看器上，直到原图/缩略图真正画出来，避免中间闪一帧空。 */
    var morphCovering by mutableStateOf(false)
    /** Compose 层报告「图已经能画了」的代数；root 收到后先淡出盖层再移除。 */
    var coverReadyToken by mutableIntStateOf(0)
    /** 入场缩略图的真实宽高比：交接给 Compose 页时先用它，避免落位瞬间按 16:9 重排。 */
    var openAspect by mutableFloatStateOf(0f)

    val dismissProgress: Float
        get() = photoDismissProgress(dragY, size.height.toFloat())

    /** 打开/关闭一次结束后复位手势态，避免下一次打开沿用上次的位移和缩放。 */
    fun resetTransient() {
        dragY = 0f
        zoom = 1f
        chromeVisible = true
        menuVisible = false
        rotationQuarter = 0
    }

    fun markCoverReady() {
        coverReadyToken++
    }

    fun startMorph(
        request: PhotoViewerRequest,
        index: Int,
        open: Boolean,
        durationMs: Long,
    ) {
        val bitmap = PhotoThumbCache.get(request.images.getOrNull(index).orEmpty())
        val origin = request.originFor(index)
        if (bitmap == null || origin == null || durationMs <= 0L) {
            // 没有缩略图缓存时退回直接显示，至少不要空白或卡在半空。
            morphParams = null
            morphCovering = false
            settled = true
            progress.floatValue = if (open) 1f else 0f
            if (!open) displayed = null
            return
        }
        morphSession++
        openAspect = if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height else 0f
        morphCovering = true
        coverReadyToken = 0
        morphParams = PhotoMorphParams(
            bitmap = bitmap,
            origin = Rect(
                origin.left.toInt(),
                origin.top.toInt(),
                origin.right.toInt(),
                origin.bottom.toInt(),
            ),
            radius = request.originRadiusPx,
            open = open,
            duration = durationMs,
            // 关闭时把当前拖动位移交给缩略图动画，避免从手指位置跳回屏幕中心再缩回。
            dragOffsetY = if (open) 0f else dragY,
            // 拖动时黑底已经变淡，关闭动画接着这个透明度走，否则会先闪回全黑。
            startDim = if (open) 1f else 1f - dismissProgress,
        )
    }
}

internal data class PhotoMorphParams(
    val bitmap: Bitmap,
    val origin: Rect,
    val radius: Float,
    val open: Boolean,
    val duration: Long,
    val dragOffsetY: Float = 0f,
    val startDim: Float = 1f,
)
