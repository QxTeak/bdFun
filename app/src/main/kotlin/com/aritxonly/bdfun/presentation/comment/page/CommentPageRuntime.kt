package com.aritxonly.bdfun.ui

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.domain.CommentQuery
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.bdfun.feature.comment.application.CommentListStateHolder
import com.aritxonly.bdfun.feature.comment.application.localCommentItem
import com.aritxonly.bdfun.feature.comment.application.CommentActionsStateHolder
import kotlinx.coroutines.*

internal class CommentPageRuntime(
    val request: CommentScreenRequest,
    val ui: CommentPagePresentationStateHolder,
    val page: VideoPagePresentationState,
    val list: CommentListStateHolder,
    val actions: CommentActionsStateHolder,
    val replies: CommentListStateHolder,
    val menu: CommentMenuStateHolder,
    val listState: LazyListState,
    val scope: CoroutineScope,
    val context: Context,
) {
    /** 刚发出的评论/回复先按本地内容显示；刷新后由 API 结果取代。 */
    fun localComment(text: String, rpid: Long, root: Long = 0L, parent: Long = 0L): CommentItem =
        localCommentItem(
            rpid = rpid,
            text = text,
            profile = page.dependencies.comments.session.value,
            oid = request.commentOid.toLongOrNull() ?: 0L,
            root = root,
            parent = parent,
            nowMs = System.currentTimeMillis(),
        )

    /** 定位/发送成功后闪一下灰：与设置页按压同款，900ms 后清掉高亮。 */
    fun flashComment(rpid: Long) {
        ui.highlightRpid = rpid
        ui.flashSeq++
        scope.launch {
            delay(900)
            if (ui.highlightRpid == rpid) ui.highlightRpid = 0L
        }
    }

    fun startReply(target: CommentItem) {
        if (!actions.isActive) return
        val root = if (target.root > 0) target.root else target.rpid
        page.replyTarget.set(target.uname, root, target.rpid)
        page.requestReplyFocus++
    }
    fun toggleLike(target: CommentItem) {
        scope.launch { actions.toggleLike(target)?.let { context.showToast(it) } }
    }
    fun openReplySheet(target: CommentItem, focusRpid: Long = 0L) {
        if (!actions.isActive) return
        ui.replySheetTarget = target
        ui.replySheetFocusRpid = focusRpid
        ui.replySheetVisible = true
        scope.launch { replies.reset(request.query(target.rpid)) }
    }
    fun loadReplySheetPage(target: CommentItem, next: Int, sortHot: Boolean) {
        scope.launch {
            if (next <= 1) replies.reset(request.query(target.rpid, sortHot))
            else replies.loadMore()
        }
    }
}
internal fun CommentScreenRequest.query(root: Long = 0L, sortHot: Boolean = true) =
    CommentQuery(commentOid, commentType, upMid, root, sortHot)

@Composable
internal fun rememberCommentPageRuntime(request: CommentScreenRequest): CommentPageRuntime {
    val ui = remember { CommentPagePresentationStateHolder() }
    val page = LocalVideoPageState.current
    val parent = rememberCoroutineScope()
    val dependencies = LocalVideoPageState.current.dependencies.comments
    val owner = dependencies.session.identity
    val generation by dependencies.likeGeneration.collectAsState()
    val context = LocalContext.current
    val list = remember(owner, generation, page, request.bvid, request.commentOid, request.commentType) { dependencies.controllers.list(request.query()) }
    val actions = remember(owner, generation, page, request.bvid, request.commentOid, request.commentType) { dependencies.controllers.actions(request.query()) }
    val replies = remember(owner, generation, page, request.bvid, request.commentOid, request.commentType) { dependencies.controllers.list(request.query()) }
    val scope = remember(list, actions, replies) {
        CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    }
    DisposableEffect(list, actions, replies) {
        onDispose { list.close(); replies.close(); actions.close(); scope.cancel() }
    }
    val menu = remember(actions) {
        CommentMenuStateHolder(context, scope, actions, request.bvid, reply@ { target ->
            if (!actions.isActive) return@reply
            val root = if (target.root > 0) target.root else target.rpid
            page.replyTarget.set(target.uname, root, target.rpid)
            page.requestReplyFocus++
        }, list::remove)
    }
    return CommentPageRuntime(request, ui, page, list, actions, replies, menu, rememberLazyListState(), scope, context)
}
