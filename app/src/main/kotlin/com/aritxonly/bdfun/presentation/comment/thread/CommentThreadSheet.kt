package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.*

/** 展开窗口继续跟随播放器底边，挂载、退场与原 220ms 减速动画保持一致。 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun CommentThreadSheet(
    input: CommentThreadInput, viewport: CommentThreadViewport, navigation: CommentThreadNavigation,
    /** 和主评论区同一套左右留白：视频详情是平铺列表（4dp），其它入口 12dp。 */
    flatList: Boolean = false,
) {
    val runtime = rememberCommentThreadRuntime(input)
    val state by key(runtime.controller) { runtime.controller.state.collectAsState() }
    val actions by key(runtime.actions) { runtime.actions.state.collectAsState() }
    val page = LocalVideoPageState.current
    val navBottom = rememberNavigationBarBottom()
    val imeVisible = WindowInsets.isImeVisible
    val mount = remember { CommentThreadMountStateHolder() }
    LaunchedEffect(Unit) { mount.mounted = true }
    DisposableEffect(page, input.visible) {
        if (input.visible) page.commentThreadVisible = true
        onDispose { page.commentThreadVisible = false }
    }
    val progress = rememberSmoothFloat(
        target = if (input.visible && mount.mounted) 1f else 0f,
        durationMs = 220f, easing = EaseOutQuint,
    )
    if (!input.visible && progress <= 0.005f) return
    val connection = rememberCommentThreadCollapseConnection(page)
    val allReplies = (state.extra + input.replies).distinctBy { it.rpid }.filterNot { it.rpid in state.deleted }
    CommentThreadEffects(runtime, input, viewport, allReplies, navigation)
    CommentThreadDialog(
        content = ThreadSheetContent(input.comment, allReplies, state.extra.size, input.loading, input.end,
            input.sortHot, actions.sending),
        layout = ThreadSheetLayout(progress, viewport.minTopPx, viewport.playerBottomDp, viewport.paneWidth, imeVisible, navBottom, flatList),
        transient = runtime.transient, listState = runtime.listState, playerCollapseConnection = connection,
        focusRequester = runtime.focusRequester,
        callbacks = ThreadSheetCallbacks(navigation.onDismiss, navigation.onToggleSort, runtime::exitReply,
            { runtime.send(input, actions.sending) }, runtime::toggleLike),
    )
    CommentThreadMenu(runtime, input)
}
