package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * 窗口级图片查看器宿主。
 *
 * 为什么必须挂在 Activity 的最外层：Telegram 的 PhotoViewer 是整个窗口的最上层，
 * 打开后盖住底栏、顶栏和状态栏。挂在页面里的话，任何画在页面之上的组件都会压住它，
 * 拖动关闭时"背后页面缩到 0.9"也只会缩那一页，不是整窗。
 *
 * 页面只需要 `LocalPhotoViewer.current.open(...)`，不需要自己创建状态或渲染覆盖层。
 */
@Composable
internal fun PhotoViewerRoot(content: @Composable () -> Unit) {
    val state = remember { PhotoViewerStateHolder() }
    val view = remember { PhotoViewerTransitionState() }
    val incoming = state.request
    val spec: AnimationSpec<Float> = rememberPhotoAnimationSpec()

    LaunchedEffect(incoming) {
        when {
            incoming != null -> view.show(incoming, spec)
            view.displayed != null -> view.hide(spec)
        }
    }
    Box(Modifier.fillMaxSize().onSizeChanged { view.size = it }) {
        // 底层页面：打开时随进度放大一点点，关闭时按同样倍数缩回；
        // 跟手拖动时再缩到 0.9。返回键的拦截放在覆盖层里（后注册才优先）。
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
            PhotoViewerOverlay(request = request, view = view, onDismiss = state::close)
        }
    }
}

/** Telegram 的入场/退场是 250ms 的 decelerate，没有回弹、没有过冲。 */
@Composable
private fun rememberPhotoAnimationSpec(): AnimationSpec<Float> {
    val animations = com.aritxonly.bdfun.AppGraph.settings.animationsEnabled
    return remember(animations) {
        tween(
            durationMillis = if (animations) PhotoViewerDurationMs else 0,
            easing = CubicBezierEasing(0f, 0f, 0.2f, 1f),
        )
    }
}

internal const val PhotoViewerDurationMs = 250

/**
 * 一次打开/关闭过程中的全部瞬时状态。
 *
 * 放在一个持有类里而不是散在 composable 中，是为了让宿主函数保持在 80 行以内，
 * 同时让"进入/退出是同一条时间线"这件事在代码上只有一个来源。
 */
internal class PhotoViewerTransitionState {
    var displayed by mutableStateOf<PhotoViewerRequest?>(null)
    val progress = Animatable(0f)
    var dragY by mutableFloatStateOf(0f)
    var zoom by mutableFloatStateOf(1f)
    var chromeVisible by mutableStateOf(true)
    var size by mutableStateOf(IntSize.Zero)

    val dismissProgress: Float
        get() = photoDismissProgress(dragY, size.height.toFloat())

    suspend fun show(request: PhotoViewerRequest, spec: AnimationSpec<Float>) {
        // 已经打开时只换内容，不重播入场——Telegram 里左右翻页就是这个行为。
        if (displayed != null && progress.value > 0.99f) {
            displayed = request
            return
        }
        displayed = request
        progress.snapTo(0f)
        dragY = 0f
        zoom = 1f
        chromeVisible = true
        progress.animateTo(1f, spec)
    }

    suspend fun hide(spec: AnimationSpec<Float>) {
        progress.animateTo(0f, spec)
        displayed = null
        dragY = 0f
        zoom = 1f
    }
}
