package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.model.comment.CommentMention
import com.aritxonly.bdfun.feature.comment.application.threadReplyMessage
import kotlinx.coroutines.*

@Composable
internal fun CommentPageEffects(runtime: CommentPageRuntime, layout: CommentScreenLayout) {
    val request = runtime.request
    LaunchedEffect(runtime.actions, layout.playerFullscreen) {
        if (layout.playerFullscreen) {
            runtime.ui.replySheetVisible = false
            runtime.menu.hide()
        }
    }
    val emotes = LocalRichTextDependencies.current.emotes
    LaunchedEffect(Unit) { emotes.ensure() }
    LaunchedEffect(runtime.list) { runtime.list.reset(request.query()) }
    CommentPaginationEffect(runtime)
    CommentSenderEffect(runtime)
    CommentFocusEffect(runtime)
}

@Composable
private fun CommentPaginationEffect(runtime: CommentPageRuntime) {
    val state by key(runtime.list) { runtime.list.state.collectAsState() }
    val listState = runtime.listState
    LaunchedEffect(runtime.list, listState, state.items.size, state.end, state.query.sortHot) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.collect { (last, count) ->
            if (runtime.list.shouldLoadMore(last, count)) runtime.list.loadMore()
        }
    }
}

@Composable
private fun CommentSenderEffect(runtime: CommentPageRuntime) {
    val request = runtime.request
    val page = runtime.page
    val ui = runtime.ui
    DisposableEffect(runtime.actions, page) {
        if (request.commentType == 1) page.aid = request.aid
        val sender: suspend (String, String?) -> Boolean = sender@ { text, pictures ->
            val root = page.replyTarget.root
            val parent = page.replyTarget.parent
            val targetName = page.replyTarget.name
            val targetMid = page.replyTarget.mid
            // 回复楼里的某个人而不是楼主时，正文明确写出对象，否则服务端只会显示成回复楼主。
            val message = threadReplyMessage(text, targetName, parent, root)
            // 同时登记成真正的提及：对方能收到 @ 通知，正文里的 @ 也能点进主页。
            val atNameToMid = if (parent > 0L && parent != root && targetMid > 0L && !targetName.isNullOrBlank()) {
                mapOf(targetName to targetMid)
            } else {
                emptyMap()
            }
            val mentions = atNameToMid.map { (name, mid) -> CommentMention(mid, name) }
            val result = runtime.actions.post(message, root, parent, pictures, atNameToMid)
            if (result == null) false else {
                page.dependencies.sentHistory.record("comment", text, request.bvid, request.title)
                page.replyTarget.clear()
                currentCoroutineContext().ensureActive()
                if (!runtime.actions.isActive) return@sender false
                if (root <= 0L && parent <= 0L) {
                    // 新主评论不立刻整页重载：先本地钉在主评论区第一条并闪灰，
                    // 等下一次刷新/重置才回到 API 的正常排序位置。
                    val local = runtime.localComment(message, result, mentions = mentions)
                    runtime.list.prependLocal(local)
                    // 主评论区不在顶端时先回到顶端，不然新评论还在屏幕外。
                    runCatching { runtime.listState.animateScrollToItem(0) }
                    currentCoroutineContext().ensureActive()
                    if (!runtime.actions.isActive) return@sender false
                    runtime.flashComment(local.rpid)
                } else {
                    // 回复别人的评论：先直接出现在对应根评论的折叠预览里并闪灰；
                    // 根评论已翻页翻出去时才退回原来的重载 + 定位。
                    val local = runtime.localComment(message, result, root, parent, mentions)
                    val attached = runtime.list.addLocalPreviewReply(root, local)
                    if (attached) {
                        runCatching { runtime.listState.animateScrollToItem(0) }
                        currentCoroutineContext().ensureActive()
                        if (!runtime.actions.isActive) return@sender false
                        runtime.flashComment(local.rpid)
                    } else {
                        runtime.list.reload()
                        currentCoroutineContext().ensureActive()
                        if (!runtime.actions.isActive) return@sender false
                        val index = if (result > 0) runtime.list.state.value.items.indexOfFirst { it.rpid == result } else -1
                        runCatching {
                            if (index >= 0) runtime.listState.animateScrollToItem(index + 1)
                            else runtime.listState.animateScrollToItem(0)
                        }
                        currentCoroutineContext().ensureActive()
                        if (!runtime.actions.isActive) return@sender false
                    }
                }
                runtime.context.showToast("回复成功")
                true
            }
        }
        page.commentSender = sender
        onDispose { if (page.commentSender === sender) page.commentSender = null }
    }
}

@Composable
private fun CommentFocusEffect(runtime: CommentPageRuntime) {
    val request = runtime.request
    val state by key(runtime.list) { runtime.list.state.collectAsState() }
    val ui = runtime.ui
    LaunchedEffect(runtime.list, state.items.size, request.focusRootRpid, request.focusTargetRpid, state.end) {
        if (request.focusRootRpid <= 0L || state.items.isEmpty()) return@LaunchedEffect
        val key = "${request.focusRootRpid}:${request.focusTargetRpid}"
        if (ui.consumedFocusKey == key) return@LaunchedEffect
        val rootIndex = state.items.indexOfFirst { it.rpid == request.focusRootRpid }
        if (rootIndex < 0) {
            // 根评论不在当前页时最多自动翻 5 页，与 main 一致。
            if (!state.end && !state.loadingMore && state.page < 5) runtime.list.loadMore()
            return@LaunchedEffect
        }
        ui.consumedFocusKey = key
        val flashTarget = request.focusTargetRpid.takeIf { it > 0L } ?: request.focusRootRpid
        // 闪灰放在 finally：滚动过程中列表状态变化会取消这个 effect，
        // 不能把“已经消费焦点”和“实际闪灰”拆成两段，否则偶发不闪。
        try {
            runCatching { runtime.listState.animateScrollToItem(rootIndex + 1) }
                .getOrElse { if (it is CancellationException) throw it }
        } finally {
            runtime.flashComment(flashTarget)
        }
        val target = request.focusTargetRpid
        if (target > 0L && target != request.focusRootRpid) runtime.openReplySheet(state.items[rootIndex])
    }
}
