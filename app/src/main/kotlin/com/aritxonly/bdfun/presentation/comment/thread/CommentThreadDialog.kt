package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun CommentThreadDialog(
    content: ThreadSheetContent, layout: ThreadSheetLayout, transient: CommentThreadPresentationStateHolder,
    listState: androidx.compose.foundation.lazy.LazyListState,
    playerCollapseConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    focusRequester: FocusRequester, callbacks: ThreadSheetCallbacks,
    animationsEnabled: Boolean = true,
) {
    val render = CommentThreadRender(content, layout, transient, listState, playerCollapseConnection, focusRequester, callbacks)
    val viewer = androidx.compose.runtime.remember { PhotoViewerStateHolder() }
    Dialog(
        onDismissRequest = callbacks.onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        // 查看大图时把楼中楼窗口临时撑到全屏，否则图片只能铺满「播放器以下」的弹窗区域，
        // 入场/退场动画也会因为窗口原点不在屏幕左上而错位（与主评论区不一致）。
        ConfigureCommentThreadWindow(
            layout.playerBottomDp, layout.minTopPx, layout.paneWidth,
            viewerOpen = viewer.request != null,
        )
        // 楼中楼是独立窗口：查看器宿主放在这里，才能盖住弹窗本身而不是跑到 Activity 后面。
        PhotoViewerRoot(animationsEnabled = animationsEnabled, state = viewer) {
            Box(modifier = Modifier.fillMaxSize()) {
                CommentThreadContent(render)
                CommentThreadComposer(transient, layout.imeVisible, layout.navBottom, focusRequester, content.sending, content.sendError, callbacks.onSend)
            }
        }
    }
}
