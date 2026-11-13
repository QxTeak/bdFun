package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import kotlinx.coroutines.launch
import kotlin.math.max
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close

/**
 * 图片层 + 黑底 + 上方 chrome。
 *
 * 这里不再对整层做统一缩放：每页自己在 [PhotoViewerPage] 里算「裁剪框 + 图片倍率」，
 * 才能让缩略图里被裁掉的部分自然展开。本层只负责黑底、chrome、跟手位移。
 */
@Composable
internal fun PhotoViewerOverlay(
    request: PhotoViewerRequest,
    view: PhotoViewerTransitionState,
    onDismiss: () -> Unit,
) {
    // 底色随进度淡入；拖动时同步变透明，松手退出会自然回到页面。
    val backgroundAlpha = (view.progress.value * (1f - view.dismissProgress)).coerceIn(0f, 1f)
    val scope = rememberCoroutineScope()

    // 返回键在这里拦：覆盖层在内容之后组合，BackHandler 后注册才优先，
    // 否则会被页面自己的返回处理器抢走，直接退出 Activity。
    BackHandler { onDismiss() }

    Box(Modifier.fillMaxSize().photoViewerDragGestures(view, scope, onDismiss)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = backgroundAlpha)))
        PhotoViewerPager(request, view, onDismiss)
        PhotoViewerChrome(request, view, onDismiss)
    }
}

/** Telegram 的顶部只有一个关闭按钮，底部是发起方提供的操作区，两者一起淡入淡出。 */
@Composable
private fun PhotoViewerChrome(
    request: PhotoViewerRequest,
    view: PhotoViewerTransitionState,
    onDismiss: () -> Unit,
) {
    val reveal = ((view.progress.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
    val alpha = if (view.chromeVisible) reveal else 0f
    // alpha 为 0 时不组合：否则看不见的关闭键仍然占据左上角的点击区域。
    if (alpha <= 0.01f) return
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            PhotoViewerTopBar(onDismiss)
        }
        request.footer?.let { footer ->
            Box(Modifier.align(Alignment.BottomCenter)) { footer(onDismiss) }
        }
    }
}

/**
 * 顶部关闭键：沿用页面返回键的尺寸规范（Miuix 图标，图标占按钮 55.2%），
 * 只是这里背后是黑底，不需要玻璃底，所以直接用白色图标。
 */
@Composable
private fun PhotoViewerTopBar(onDismiss: () -> Unit) {
    val buttonSize = TeakChromeDefaults.BackButtonSize
    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Close,
                contentDescription = "关闭",
                tint = Color.White,
                modifier = Modifier.size(buttonSize * 0.552f),
            )
        }
    }
}

@Composable
private fun PhotoViewerPager(
    request: PhotoViewerRequest,
    view: PhotoViewerTransitionState,
    onDismiss: () -> Unit,
) {
    val pager = rememberPagerState(
        initialPage = request.index.coerceIn(0, request.images.lastIndex),
        pageCount = { request.images.size },
    )
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        // 起点矩形只属于点开的那一张：滑到别的图后不能再借用它的位置，
        // 否则退出时会往第一张缩略图飞。
        val isSource = page == request.index
        PhotoViewerPage(
            url = request.images[page],
            request = request,
            view = view,
            origin = if (isSource) request.origin else null,
            originRadiusPx = if (isSource) request.originRadiusPx else 0f,
            onDismiss = onDismiss,
        )
    }
}

/**
 * 下滑关闭：镜像 Telegram 的判定——拖到半屏算走完全程，
 * 松手时看位移和速度，任一到阈值就退出。
 */
private fun Modifier.photoViewerDragGestures(
    view: PhotoViewerTransitionState,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit,
): Modifier = pointerInput(view.progress.value, view.zoom) {
    if (view.progress.value < 0.99f || view.zoom > 1.01f) return@pointerInput
    var startTime = 0L
    var totalY = 0f
    fun settle() {
        scope.launch {
            animate(view.dragY, 0f, animationSpec = spring()) { value, _ -> view.dragY = value }
        }
    }
    detectVerticalDragGestures(
        onDragStart = {
            startTime = System.currentTimeMillis()
            totalY = 0f
        },
        onVerticalDrag = { change, dragAmount ->
            change.consume()
            totalY += dragAmount
            view.dragY = totalY
        },
        onDragEnd = {
            val elapsed = max(1L, System.currentTimeMillis() - startTime)
            val velocity = totalY / elapsed * 1000f
            val progress = photoDismissProgress(totalY, view.size.height.toFloat())
            if (photoShouldDismiss(progress, velocity)) onDismiss() else settle()
        },
        onDragCancel = { settle() },
    )
}
