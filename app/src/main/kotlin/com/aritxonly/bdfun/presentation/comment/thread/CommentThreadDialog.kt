package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun CommentThreadDialog(
    content: ThreadSheetContent, layout: ThreadSheetLayout, transient: CommentThreadPresentationStateHolder,
    listState: androidx.compose.foundation.lazy.LazyListState,
    playerCollapseConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    focusRequester: FocusRequester, callbacks: ThreadSheetCallbacks,
) {
    val render = CommentThreadRender(content, layout, transient, listState, playerCollapseConnection, focusRequester, callbacks)
    Dialog(
        onDismissRequest = callbacks.onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        ConfigureCommentThreadWindow(layout.playerBottomDp, layout.minTopPx, layout.paneWidth)
        Box(modifier = Modifier.fillMaxSize()) {
            CommentThreadContent(render)
            CommentThreadComposer(transient, layout.imeVisible, layout.navBottom, focusRequester, content.sending, content.sendError, callbacks.onSend)
        }
    }
}
