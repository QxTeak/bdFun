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
        scope.launch {
            val result = controller.send(text, target.rpid, dependencies.comments.session.value, System::currentTimeMillis)
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
                // LazyColumn 保持原可见条目的锚点，新回复插到顶部后要主动展示。
                listState.animateScrollToItem(2)
            } else context.showToast(actions.state.value.sendError?.messageOr("发送失败，请稍后重试") ?: "发送失败，请稍后重试")
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
