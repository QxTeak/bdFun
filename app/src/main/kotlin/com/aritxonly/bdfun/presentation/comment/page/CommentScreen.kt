package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import kotlinx.coroutines.launch

@Composable
internal fun CommentScreen(
    request: CommentScreenRequest, onBack: () -> Unit,
    layout: CommentScreenLayout = CommentScreenLayout(), header: CommentScreenHeader? = null,
) {
    val runtime = rememberCommentPageRuntime(request)
    CommentPageEffects(runtime, layout)
    if (layout.trackBackProgress) TrackTeakPageScroll(runtime.listState)
    if (!layout.bare) BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize()) {
        CommentPageList(runtime, layout, header)
        CommentMenu(runtime.menu, LocalVideoPageState.current.dependencies.comments.session.value?.mid ?: 0L)
        CommentPageThread(runtime, layout)
    }
}

@Composable
private fun CommentPageList(
    runtime: CommentPageRuntime, layout: CommentScreenLayout, header: CommentScreenHeader?,
) {
    val state by key(runtime.list) { runtime.list.state.collectAsState() }
    val ui = runtime.ui
    CommentListView(
        state = CommentListViewState(state.items, state.total, state.loading, state.loadingMore, state.end,
            state.error?.messageOr("评论加载失败"), state.query.sortHot, ui.highlightRpid, ui.disliked),
        layout = CommentListViewLayout(layout.bare, runtime.request.title, layout.topInset,
            layout.bottomPadding, layout.showLoading, header, layout.flatList),
        actions = CommentListViewActions(
            onSort = { runtime.scope.launch { runtime.list.reset(state.query.copy(sortHot = !state.query.sortHot)) } },
            onLike = runtime::toggleLike,
            onDislike = { ui.disliked[it.rpid] = ui.disliked[it.rpid] != true },
            onReply = runtime::startReply, onMenu = runtime.menu::open, onOpenReplies = runtime::openReplySheet,
        ),
        listState = runtime.listState,
    )
}
