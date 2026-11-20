package com.aritxonly.bdfun.ui

import android.content.Context
import com.aritxonly.bdfun.core.common.messageOr
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.aritxonly.bdfun.core.domain.CommentQuery
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.bdfun.feature.comment.application.CommentThreadStateHolder
import com.aritxonly.bdfun.feature.comment.application.threadReplyMessage
import kotlinx.coroutines.*

internal class CommentThreadRuntime(
    val controller: CommentThreadStateHolder,
    val transient: CommentThreadPresentationStateHolder,
    val scope: CoroutineScope,
    val context: Context,
    val listState: LazyListState,
    val focusRequester: FocusRequester,
    private val focusManager: FocusManager,
    private val keyboard: SoftwareKeyboardController?,
    private val dependencies: VideoPageDependencies,
) {
    val actions get() = controller.actions
    fun exitReply() {
        transient.replyTarget = null
        transient.draft = ""
        focusManager.clearFocus()
        keyboard?.hide()
    }
    fun send(input: CommentThreadInput, sending: Boolean) {
        if (!controller.isActive) return
        val target = transient.replyTarget ?: return
        val text = transient.draft.trim()
        if (text.isEmpty() || sending) return
        // 回复楼里的某个人（不是楼主）时，正文带上对象：
        // 服务端只按根楼归档，不写的话发出去看起来就是直接回复楼主。
        val message = threadReplyMessage(text, target.uname, target.rpid, input.comment.rpid)
        // 同一个 @ 还要登记成真正的提及，否则对方收不到 @ 通知、文字也点不进主页。
        val atNameToMid = if (target.rpid != input.comment.rpid && target.mid > 0L) {
            mapOf(target.uname to target.mid)
        } else {
            emptyMap()
        }
        scope.launch {
            val result = controller.send(
                message, target.rpid, dependencies.comments.session.value,
                atNameToMid, System::currentTimeMillis,
            )
            currentCoroutineContext().ensureActive()
            if (!controller.isActive) return@launch
            if (result != null) {
                dependencies.sentHistory.record("comment", text, input.bvid, input.videoTitle)
                if (transient.draft.trim() == text && transient.replyTarget?.rpid == target.rpid) {
                    transient.draft = ""
                    transient.replyTarget = null
                }
                keyboard?.hide()
                context.showToast("回复成功")
                // 新回复已插到被回复对象下面：滚过去时把父评论留在屏幕顶部，别把整页甩回列表头。
                val local = controller.state.value.extra.lastOrNull()
                val merged = mergeThreadReplies(input.replies, controller.state.value.extra, controller.state.value.deleted)
                val index = if (local != null) merged.indexOfFirst { it.rpid == local.rpid } else -1
                if (index >= 0) {
                    // 前两项是「原评论」和「共 N 条评论」。
                    runCatching { listState.animateScrollToItem(if (index > 0) index + 1 else 0) }
                    currentCoroutineContext().ensureActive()
                    if (controller.isActive && local != null) flash(local.rpid)
                }
            } else context.showToast(actions.state.value.sendError?.messageOr("发送失败，请稍后重试") ?: "发送失败，请稍后重试")
        }
    }
    /** 定位/发送成功后闪一下灰：与设置页按压同款，900ms 后清掉高亮。 */
    fun flash(rpid: Long) {
        transient.flashRpid = rpid
        transient.flashSeq++
        scope.launch {
            delay(900)
            if (transient.flashRpid == rpid) transient.flashRpid = 0L
        }
    }
    fun toggleLike(item: CommentItem) {
        scope.launch { actions.toggleLike(item)?.let { context.showToast(it) } }
    }
    fun dismissMenu(after: (() -> Unit)? = null) {
        transient.menuVisible = false
        scope.launch {
            delay(360)
            transient.menuReply = null
            if (controller.isActive) after?.invoke()
        }
    }
}

@Composable
internal fun rememberCommentThreadRuntime(input: CommentThreadInput): CommentThreadRuntime {
    val dependencies = LocalVideoPageState.current.dependencies.comments
    val owner = dependencies.session.identity
    val generation by dependencies.likeGeneration.collectAsState()
    val page = LocalVideoPageState.current
    val parent = rememberCoroutineScope()
    val actions = remember(owner, generation, page, input.commentOid, input.commentType, input.comment.rpid) {
        dependencies.controllers.actions(CommentQuery(input.commentOid, input.commentType, 0, input.comment.rpid))
    }
    val controller = remember(actions) { CommentThreadStateHolder(actions, input.comment.rpid) }
    val scope = remember(controller) { CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job])) }
    DisposableEffect(controller) { onDispose { controller.close(); actions.close(); scope.cancel() } }
    val transient = remember(owner, input.commentOid, input.commentType, input.comment.rpid) {
        CommentThreadPresentationStateHolder()
    }
    return CommentThreadRuntime(controller, transient, scope, LocalContext.current, rememberLazyListState(),
        remember { FocusRequester() }, LocalFocusManager.current, LocalSoftwareKeyboardController.current, page.dependencies)
}
