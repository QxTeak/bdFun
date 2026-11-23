package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import android.content.pm.ActivityInfo
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
    content: @Composable () -> Unit,
) {
    val state = remember { PhotoViewerStateHolder() }
    val view = remember { PhotoViewerTransitionState() }
    val incoming = state.request
    val context = LocalContext.current
    // 查看器打开期间跟传感器转向，不受系统“自动旋转”开关限制；关闭后恢复进入前的方向策略。
    DisposableEffect(incoming != null) {
        val activity = context.findHostActivity()
        if (incoming != null && activity != null) {
            val previous = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            onDispose { activity.requestedOrientation = previous }
        } else {
            onDispose { }
        }
    }
    val durationMs = if (animationsEnabled) PhotoViewerDurationMs else 0L
    val viewerBackdrop = rememberLayerBackdrop {
        drawRect(androidx.compose.ui.graphics.Color.Black)
        drawContent()
    }
    var morphViewRef by remember { mutableStateOf<PhotoMorphView?>(null) }

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
                view.startMorph(request, request.index, open = true, durationMs)
            }
            request == null && current != null -> {
                view.settled = false
                view.startMorph(current, view.pageIndex, open = false, durationMs)
            }
        }
    }
    // 原图/缩略图迟迟没画出来时，缩略图不能永远盖着；超时后露出 Compose 层。
    LaunchedEffect(view.settled, view.morphCovering) {
        if (view.settled && view.morphCovering) {
            kotlinx.coroutines.delay(600)
            if (view.morphCovering) {
                morphViewRef?.fadeOut { view.morphCovering = false } ?: run { view.morphCovering = false }
            }
        }
    }
    // Compose 层说图已经能画了：等它真正画出一两帧后再淡出盖层，杜绝中间闪一帧。
    PhotoCoverReadyEffect(view) { morphViewRef }

    // 入场动画/缩略图盖层期间点击也要能立刻关闭，不能等原图加载完。
    val dismissOnTap = view.displayed != null && (!view.settled || view.morphCovering)
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { view.size = it }
            .then(if (dismissOnTap) Modifier.pointerInput(Unit) {
                detectTapGestures { state.close() }
            } else Modifier),
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
        view.displayed?.let { request ->
            if (view.settled) {
                PhotoViewerSettledLayer(request, view, state, viewerBackdrop)
            }
            // 动画结束后缩略图仍盖在最上面，直到 Compose 层真正把原图/缩略图画出来。
            if ((!view.settled || view.morphCovering) && view.morphParams != null) {
                PhotoMorphLayer(view, state) { morphViewRef = it }
            }
        }
    }
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
    // 和 Telegram 一样画在同一个窗口里；楼中楼有自己的一层宿主。
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
                onPageChanged = { view.pageIndex = it },
            )
        }
    }
}

@Composable
private fun PhotoMorphLayer(
    view: PhotoViewerTransitionState,
    state: PhotoViewerStateHolder,
    onView: (PhotoMorphView?) -> Unit,
) {
    val params = view.morphParams ?: return
    key(view.morphSession) {
        AndroidView(
            factory = { context ->
                PhotoMorphView(context).apply {
                    onView(this)
                    onProgress = { view.progress.floatValue = it }
                    onFinished = {
                        if (state.request == null) {
                            view.displayed = null
                            view.progress.floatValue = 0f
                            view.morphParams = null
                            view.morphCovering = false
                            // 关闭动画结束后清掉手势残留，下一次打开从干净状态开始。
                            view.resetTransient()
                        } else {
                            view.settled = true
                            view.progress.floatValue = 1f
                        }
                    }
                    start(params.bitmap, params.origin, params.radius, params.open,
                        durationMs = params.duration, dragOffsetY = params.dragOffsetY, startDim = params.startDim)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Telegram 的入场/退场是 200ms 的 AccelerateDecelerate，没有回弹、没有过冲。 */
internal const val PhotoViewerDurationMs = 200L


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
    var morphParams by mutableStateOf<PhotoMorphParams?>(null)
    var morphSession by mutableIntStateOf(0)
    /** 缩略图继续盖在查看器上，直到原图/缩略图真正画出来，避免中间闪一帧空。 */
    var morphCovering by mutableStateOf(false)
    /** Compose 层报告“图已经能画了”的代数；Root 收到后先淡出盖层再移除。 */
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
