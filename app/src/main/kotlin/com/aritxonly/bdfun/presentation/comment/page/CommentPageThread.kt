package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun CommentPageThread(runtime: CommentPageRuntime, layout: CommentScreenLayout) {
    val state by key(runtime.replies) { runtime.replies.state.collectAsState() }
    val request = runtime.request
    val ui = runtime.ui
    val density = LocalDensity.current
    val statusTop = rememberStatusBarTop()
    // 动态没有播放器，面板顶边最多停在返回键下面。
    val threadMinTopPx = if (layout.threadPaneWidth != null && layout.threadPlayerBottom != null) {
        with(density) { layout.threadPlayerBottom.toPx() }
    } else if (layout.bare) with(density) { (statusTop + 56.dp).toPx() } else 0f
    val sheetTarget = ui.replySheetTarget
    if (sheetTarget != null && !layout.playerFullscreen) {
        CommentThreadSheet(
            input = CommentThreadInput(
                visible = ui.replySheetVisible, comment = sheetTarget, replies = state.items,
                loading = state.loading || state.loadingMore, end = state.end,
                commentOid = request.commentOid, commentType = request.commentType,
                bvid = request.bvid, videoTitle = request.title, sortHot = state.query.sortHot,
            ),
            viewport = CommentThreadViewport(
                focusRpid = ui.replySheetFocusRpid.takeIf { it > 0L }
                    ?: if (ui.consumedFocusKey == "${request.focusRootRpid}:${request.focusTargetRpid}") {
                        request.focusTargetRpid
                    } else 0L,
                minTopPx = threadMinTopPx, playerBottomDp = layout.threadPlayerBottom, paneWidth = layout.threadPaneWidth,
            ),
            navigation = CommentThreadNavigation(
                onToggleSort = { runtime.scope.launch {
                    runtime.replies.reset(request.query(sheetTarget.rpid, !state.query.sortHot))
                } },
                onLoadMore = { runtime.loadReplySheetPage(sheetTarget, state.page + 1, state.query.sortHot) },
                onDismiss = {
                    // 保留 target，等待原面板退场弹簧结束。
                    ui.replySheetVisible = false
                    ui.replySheetFocusRpid = 0L
                },
            ),
            flatList = layout.flatList,
        )
    }
}
