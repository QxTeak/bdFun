package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import kotlinx.coroutines.launch
import kotlin.math.max
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.RotateLeft
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
    onPageChanged: (Int) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    // 关闭时只把 visible 置 false，锚点保留：否则退场动画会瞬移到 (0,0)，
    // 看起来就是"点空白后菜单乱跳一下"。
    var menuAnchor by remember { mutableStateOf(Rect.Zero) }
    var menuUrl by remember { mutableStateOf("") }
    val menuState = remember { com.aritxonly.teak.common.navigation.TeakMorphMenuState() }
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
        // 底色随进度淡入、拖动时同步变透明；在绘制阶段读取，入场动画不重组整层。
        Box(Modifier.fillMaxSize().drawBehind {
            val progress = view.progress.value.coerceIn(0f, 1f)
            val dismiss = photoDismissProgress(view.dragY, view.size.height.toFloat())
            drawRect(Color.Black.copy(alpha = (progress * (1f - dismiss)).coerceIn(0f, 1f)))
        })
        PhotoViewerPager(request, view, onDismiss, onPageChanged) { url, offset ->
            // 和长按评论完全同一套：菜单锚在手指按下的那个点上，位置交给共用菜单自己收边。
            menuUrl = url
            menuAnchor = Rect(offset.x, offset.y, offset.x, offset.y)
            view.menuVisible = true
        }
        PhotoViewerChrome(request, view, onDismiss)
    }

    val images = request.images
    // 退场也要继续录图片层：否则一关菜单就停掉 viewerBackdrop，
    // 玻璃面板会在回弹动画第一帧直接变成透明/纯色，看起来像"面板瞬间没了"。
    val dismissMenu: () -> Unit = {
        menuState.close()
        scope.launch {
            // 回弹约 200ms；只多留一帧余量就卸载窗口，避免一个不可触摸的 Dialog 长时间挂在查看器上面。
            kotlinx.coroutines.delay(400)
            view.menuVisible = false
        }
    }
    PhotoImageMenu(
        visible = view.menuVisible,
        anchor = menuAnchor,
        state = menuState,
        onDismiss = dismissMenu,
        onSaveCurrent = { save(listOf(menuUrl)) },
        onSaveAll = { save(images) },
        // 图片举报接口还没接，先如实提示，不做假成功。
        onReport = { context.showToast("举报功能稍后接入") },
    )
}

private val PhotoDangerRed = Color(0xFFE5484D)

/** 长按图片弹出的菜单：和视频卡片同一套玻璃形变动画，从手指位置展开。 */
@Composable
private fun PhotoImageMenu(
    visible: Boolean,
    anchor: Rect,
    state: com.aritxonly.teak.common.navigation.TeakMorphMenuState,
    onDismiss: () -> Unit,
    onSaveCurrent: () -> Unit,
    onSaveAll: () -> Unit,
    onReport: () -> Unit,
) {
    var mounted by remember { mutableStateOf(visible) }
    if (visible) mounted = true
    // 收起动画一开始 state.open 就变 false；Dialog 仍要留着画完回弹，
    // 但输入必须立刻还给查看器。
    val menuOpen = state.isOpen
    LaunchedEffect(visible) {
        if (visible) {
            // 先以关闭态组合一帧，再张开，Miuix 才能播形变入场。
            androidx.compose.runtime.withFrameNanos { }
            state.openWithAnchorCopy(false)
        } else if (mounted) {
            state.close()
            kotlinx.coroutines.delay(400)
            mounted = false
        }
    }
    if (!mounted) return
    // 图片查看器本身是铺满窗口的录制层，玻璃菜单必须放进独立窗口，
    // 否则形变面板会被自己的 Backdrop 采样，出现 RenderNode 递归。
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect {
            dialogWindow?.setDimAmount(0f)
            dialogWindow?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
            )
            dialogWindow?.setWindowAnimations(0)
            // 退场一开始就释放窗口输入，菜单继续播回弹，但下一次点击能立即落到查看器/页面上。
            val releaseInput = android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            if (menuOpen) dialogWindow?.clearFlags(releaseInput) else dialogWindow?.addFlags(releaseInput)
        }
        Box(Modifier.fillMaxSize()) {
            com.aritxonly.teak.common.navigation.TeakMorphPointAnchor(
                state,
                androidx.compose.ui.geometry.Offset(anchor.left, anchor.top),
            )
            com.aritxonly.teak.common.navigation.TeakMorphMenuHost(
                state,
                onDismiss = onDismiss,
                minWidth = 168.dp,
                maxWidth = 216.dp,
                maxHeight = 420.dp,
            ) { close ->
                top.yukonga.miuix.kmp.glass.GlassPopupItem(
                    text = "保存该图片",
                    onClick = {
                        close { onSaveCurrent() }
                        onDismiss()
                    },
                )
                top.yukonga.miuix.kmp.glass.GlassPopupItem(
                    text = "保存全部图片",
                    onClick = {
                        close { onSaveAll() }
                        onDismiss()
                    },
                )
                top.yukonga.miuix.kmp.glass.GlassPopupItem(
                    text = "举报",
                    contentColor = PhotoDangerRed,
                    onClick = {
                        close { onReport() }
                        onDismiss()
                    },
                )
            }
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
    // 只有跨过“该显示/该隐藏”的阈值才重组一次；alpha 在绘制阶段读取。
    val chromeShown by remember {
        androidx.compose.runtime.derivedStateOf {
            view.chromeVisible && ((view.progress.value - 0.6f) / 0.4f) > 0.01f
        }
    }
    // 不可见时不组合：否则看不见的关闭键仍然占据左上角的点击区域。
    if (!chromeShown) return
    Box(Modifier.fillMaxSize().graphicsLayer {
        this.alpha = if (view.chromeVisible) {
            ((view.progress.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
        } else {
            0f
        }
    }) {
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            PhotoViewerTopBar(onDismiss)
        }
        // 底部中间是手动旋转键（Miuix 图标），下面接发起方自己的操作区。
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(if (request.footer == null) Modifier.navigationBarsPadding() else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PhotoViewerRotateButton(onClick = {
                view.rotationQuarter = (view.rotationQuarter + 1) % 4
            })
            request.footer?.let { footer ->
                Box(Modifier.fillMaxWidth()) { footer(onDismiss) }
            }
        }
    }
}

/** 底部旋转键：黑底上一枚白色 Miuix 旋转图标，点一次顺时针转 90°。 */
@Composable
private fun PhotoViewerRotateButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(bottom = 10.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.28f))
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.RotateLeft,
            contentDescription = "旋转图片",
            tint = Color.White,
            modifier = Modifier.size(23.dp),
        )
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
    onPageChanged: (Int) -> Unit,
    onLongPress: (String, androidx.compose.ui.geometry.Offset) -> Unit,
) {
    val pager = rememberPagerState(
        initialPage = request.index.coerceIn(0, request.images.lastIndex),
        pageCount = { request.images.size },
    )
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { onPageChanged(it) }
    }
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
@Composable
private fun Modifier.photoViewerDragGestures(
    view: PhotoViewerTransitionState,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit,
): Modifier {
    // 入场动画期间 progress 每帧都在变；用 threshold state 做 key，
    // 手势识别器只在“完全展开/放大”状态切换时重建，而不是每帧重建。
    val settled by remember {
        androidx.compose.runtime.derivedStateOf {
            view.progress.value >= 0.99f && view.zoom <= 1.01f
        }
    }
    return pointerInput(settled) {
    if (!settled) return@pointerInput
    var startTime = 0L
    var totalY = 0f
    // 回弹用的弹簧要能被下一次按下打断，否则和新的拖动同时改 dragY，画面会抽。
    var settleJob: kotlinx.coroutines.Job? = null
    fun settle() {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(view.dragY, 0f, animationSpec = spring()) { value, _ -> view.dragY = value }
            settleJob = null
        }
    }
    detectVerticalDragGestures(
        onDragStart = {
            settleJob?.cancel()
            settleJob = null
            startTime = System.currentTimeMillis()
            totalY = view.dragY
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
}
