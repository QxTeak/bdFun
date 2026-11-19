package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.model.comment.CommentItem
import kotlinx.coroutines.delay

@Composable
internal fun CommentThreadEffects(
    runtime: CommentThreadRuntime, input: CommentThreadInput, viewport: CommentThreadViewport,
    allReplies: List<CommentItem>, navigation: CommentThreadNavigation,
) {
    val listState = runtime.listState
    val comment = input.comment
    val commentOid = input.commentOid
    val commentType = input.commentType
    val end = input.end
    val loading = input.loading
    val focusRpid = viewport.focusRpid
    val onDismiss = navigation.onDismiss
    val onLoadMore = navigation.onLoadMore
    val focusRequester = runtime.focusRequester
    // 返回键：先退出回复框；没有回复框时才关闭整个展开页。
    androidx.activity.compose.BackHandler {
        if (runtime.transient.replyTarget != null) {
            runtime.exitReply()
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(runtime.controller, listState, commentOid, commentType, comment.rpid, allReplies.size, end, loading) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.collect { (last, count) ->
            if (!end && !loading && allReplies.isNotEmpty() && last >= count - 1) {
                onLoadMore()
            }
        }
    }

    // 消息页定位到楼中楼：回复列表里找到这条就直接滚过去。
    LaunchedEffect(runtime.controller, comment.rpid, focusRpid, allReplies.size, loading, end) {
        if (focusRpid <= 0L || runtime.transient.consumedFocusRpid == focusRpid) return@LaunchedEffect
        val index = allReplies.indexOfFirst { it.rpid == focusRpid }
        if (index >= 0) {
            runtime.transient.consumedFocusRpid = focusRpid
            // 闪灰放在 finally：滚动期间 allReplies/loading 变化会让这个 effect 重启，
            // 先把“已消费”写掉再滚，闪灰必须无条件执行，否则就会出现偶发不闪。
            try {
                // 前面有「原评论」和「共 N 条评论」两个 item。
                runCatching { listState.animateScrollToItem(index + 2) }
                    .getOrElse { if (it is kotlinx.coroutines.CancellationException) throw it }
            } finally {
                runtime.flash(focusRpid)
            }
        } else if (!end && !loading) {
            onLoadMore()
        } else if (end && !loading) {
            // 目标回复确实不在列表里（已删除/被折叠）：至少闪一下它所在的根评论楼。
            runtime.transient.consumedFocusRpid = focusRpid
            runtime.flash(comment.rpid)
        }
    }

    LaunchedEffect(runtime.controller, runtime.transient.replyTarget) {
        if (runtime.transient.replyTarget != null) {
            delay(60)
            runCatching { focusRequester.requestFocus() }
        }
    }

}
