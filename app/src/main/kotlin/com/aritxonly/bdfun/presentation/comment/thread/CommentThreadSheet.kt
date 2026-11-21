package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.common.messageOr

/** 展开窗口继续跟随播放器底边，挂载、退场与原 220ms 减速动画保持一致。 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun CommentThreadSheet(
    input: CommentThreadInput, viewport: CommentThreadViewport, navigation: CommentThreadNavigation,
    /** 和主评论区同一套左右留白：视频详情是平铺列表（4dp），其它入口 12dp。 */
    flatList: Boolean = false,
    animationsEnabled: Boolean = true,
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
    // 展开/关闭比原来慢 20%（220ms → 264ms），并换成更柔和的先快后慢曲线，
    // 收起时不会一下子"抽"走。
    val progress = rememberSmoothFloat(
        target = if (input.visible && mount.mounted) 1f else 0f,
        durationMs = 264f, easing = EaseOutSoft,
    )
    if (!input.visible && progress <= 0.005f) return
    val connection = rememberCommentThreadCollapseConnection(page)
    val allReplies = mergeThreadReplies(input.replies, state.extra, state.deleted)
    CommentThreadEffects(runtime, input, viewport, allReplies, navigation)
    // 展开页里的根评论、回复行共用同一个高亮目标；定位失败时也能退化到根评论。
    androidx.compose.runtime.CompositionLocalProvider(
        LocalCommentFlashTarget provides CommentFlashTarget(
            runtime.transient.flashRpid, runtime.transient.flashSeq,
        ),
    ) {
    CommentThreadDialog(
        content = ThreadSheetContent(input.comment, allReplies, state.extra.size, input.loading, input.end,
            input.sortHot, actions.sending, actions.sendError?.messageOr("发送失败，请重试")),
        layout = ThreadSheetLayout(progress, viewport.minTopPx, viewport.playerBottomDp, viewport.paneWidth, imeVisible, navBottom, flatList),
        transient = runtime.transient, listState = runtime.listState, playerCollapseConnection = connection,
        focusRequester = runtime.focusRequester,
        callbacks = ThreadSheetCallbacks(navigation.onDismiss, navigation.onToggleSort, runtime::exitReply,
            { runtime.send(input, actions.sending) }, runtime::toggleLike),
        animationsEnabled = animationsEnabled,
    )
    }
    CommentThreadMenu(runtime, input)
}
