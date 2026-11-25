package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
internal fun CommentThreadDialog(
    content: ThreadSheetContent, layout: ThreadSheetLayout, transient: CommentThreadPresentationStateHolder,
    listState: androidx.compose.foundation.lazy.LazyListState,
    playerCollapseConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    focusRequester: FocusRequester, callbacks: ThreadSheetCallbacks,
    animationsEnabled: Boolean = true,
) {
    val render = CommentThreadRender(content, layout, transient, listState, playerCollapseConnection, focusRequester, callbacks)
    // 楼中楼自己的图片查看器状态：列表里的缩略图只负责发起请求。
    val viewer = remember { PhotoViewerStateHolder() }
    var viewerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(viewer.request) {
        if (viewer.request != null) {
            viewerVisible = true
        } else {
            // 退场形变约 200ms，等它播完再移除全屏覆盖层。
            delay(320)
            viewerVisible = false
        }
    }
    Dialog(
        onDismissRequest = callbacks.onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        ConfigureCommentThreadWindow(layout.playerBottomDp, layout.minTopPx, layout.paneWidth)
        // 楼中楼是独立窗口：查看器宿主放在这里，才能盖住弹窗本身而不是跑到 Activity 后面。
        PhotoViewerRoot(animationsEnabled = animationsEnabled) {
            CompositionLocalProvider(LocalPhotoViewer provides viewer) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CommentThreadContent(render)
                    CommentThreadComposer(transient, layout.imeVisible, layout.navBottom, focusRequester, content.sending, content.sendError, callbacks.onSend)
                }
            }
        }
    }
    // 大图单独放在一个铺满屏幕的窗口里：楼中楼面板本身不改变几何，
    // 查看器的入场/退场与主评论区一致（缩略图 bounds 本来就是屏幕坐标）。
    if (viewerVisible) {
        Dialog(
            onDismissRequest = viewer::close,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false,
            ),
        ) {
            ConfigureFullscreenViewerWindow()
            PhotoViewerRoot(animationsEnabled = animationsEnabled, state = viewer) {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}

/** 全屏、无遮罩、无窗口动画：查看器自己负责背景与形变。 */
@Composable
private fun ConfigureFullscreenViewerWindow() {
    val view = androidx.compose.ui.platform.LocalView.current
    val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
    androidx.compose.runtime.SideEffect {
        window?.setDimAmount(0f)
        window?.setWindowAnimations(0)
        val lp = window?.attributes ?: return@SideEffect
        lp.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
        lp.x = 0
        lp.y = 0
        window.attributes = lp
    }
}
