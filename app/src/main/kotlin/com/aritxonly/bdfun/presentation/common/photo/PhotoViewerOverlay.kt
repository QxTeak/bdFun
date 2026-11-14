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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.settings.TeakDropdownPanel
import kotlinx.coroutines.launch
import kotlin.math.max
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val context = androidx.compose.ui.platform.LocalContext.current
    // 关闭时只把 visible 置 false，锚点保留：否则退场动画会瞬移到 (0,0)，
    // 看起来就是"点空白后菜单乱跳一下"。
    var menuVisible by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(Rect.Zero) }
    var menuUrl by remember { mutableStateOf("") }
    val menuScope = rememberCoroutineScope()

    fun save(urls: List<String>) {
        if (urls.isEmpty()) return
        menuScope.launch {
            val (saved, total) = ImageSaver.save(context, urls)
            context.showToast(
                when {
                    saved == total && total == 1 -> "已保存到相册"
                    saved == total -> "已保存 $saved 张图片"
                    else -> "已保存 $saved/$total 张，部分失败"
                },
            )
        }
    }

    // 返回键在这里拦：覆盖层在内容之后组合，BackHandler 后注册才优先，
    // 否则会被页面自己的返回处理器抢走，直接退出 Activity。
    BackHandler { onDismiss() }

    Box(Modifier.fillMaxSize().photoViewerDragGestures(view, scope, onDismiss)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = backgroundAlpha)))
        PhotoViewerPager(request, view, onDismiss) { url, offset ->
            // 和长按评论完全同一套：菜单锚在手指按下的那个点上，位置交给共用菜单自己收边。
            menuUrl = url
            menuAnchor = Rect(offset.x, offset.y, offset.x, offset.y)
            menuVisible = true
        }
        PhotoViewerChrome(request, view, onDismiss)
    }

    val images = request.images
    PhotoImageMenu(
        visible = menuVisible,
        anchor = menuAnchor,
        onDismiss = { menuVisible = false },
        onSaveCurrent = { save(listOf(menuUrl)) },
        onSaveAll = { save(images) },
        // 图片举报接口还没接，先如实提示，不做假成功。
        onReport = { context.showToast("举报功能稍后接入") },
    )
}

private val PhotoDangerRed = Color(0xFFE5484D)

/** 长按图片弹出的菜单：和长按评论同一个玻璃菜单（TeakGlassMenu），不是设置页那种 Dropdown。 */
@Composable
private fun PhotoImageMenu(
    visible: Boolean,
    anchor: Rect,
    onDismiss: () -> Unit,
    onSaveCurrent: () -> Unit,
    onSaveAll: () -> Unit,
    onReport: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    TeakGlassMenu(
        visible = visible,
        onDismiss = onDismiss,
        anchorInWindow = anchor,
        minWidth = 168.dp,
        maxWidth = 216.dp,
        maxHeight = 420.dp,
    ) {
        CommentMenuRow("保存该图片", MenuGlyphKind.Copy, colors.onBackground) {
            onDismiss(); onSaveCurrent()
        }
        CommentMenuRow("保存全部图片", MenuGlyphKind.Copy, colors.onBackground) {
            onDismiss(); onSaveAll()
        }
        CommentMenuRow("举报", MenuGlyphKind.Report, PhotoDangerRed) {
            onDismiss(); onReport()
        }
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
    onLongPress: (String, androidx.compose.ui.geometry.Offset) -> Unit,
) {
    val pager = rememberPagerState(
        initialPage = request.index.coerceIn(0, request.images.lastIndex),
        pageCount = { request.images.size },
    )
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        // 每一页用自己的缩略图矩形收拢：滑到第二张再退出，就落到第二张的位置。
        val origin = request.originFor(page)
        PhotoViewerPage(
            url = request.images[page],
            request = request,
            view = view,
            origin = origin,
            originRadiusPx = if (origin != null) request.originRadiusPx else 0f,
            onDismiss = onDismiss,
            onLongPress = onLongPress,
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
