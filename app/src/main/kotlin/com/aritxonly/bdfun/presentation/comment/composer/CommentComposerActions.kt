package com.aritxonly.bdfun.ui

import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.aritxonly.bdfun.feature.comment.application.CommentComposerStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class CommentComposerActions(
    private val controller: CommentComposerStateHolder,
    private val page: VideoPagePresentationState,
    private val scope: CoroutineScope,
    private val focusRequester: FocusRequester,
    private val focusManager: FocusManager,
    private val keyboard: SoftwareKeyboardController?,
    val pickImage: () -> Unit,
) {
    fun setDraft(value: String) = controller.setDraft(value)
    fun pickEmoji(value: String) = controller.appendEmoji(value)
    fun focusChanged(focused: Boolean) {
        if (focused) {
            controller.setEditing(true); page.replyEditing = true
        } else if (!controller.state.value.showEmoji) {
            controller.setEditing(false); page.replyEditing = false
        }
    }
    fun focusReply() {
        runCatching { focusRequester.requestFocus() }
        controller.setEditing(true); page.replyEditing = true
    }
    fun toggleEmoji() {
        controller.toggleEmoji()
        page.replyEditing = true
        runCatching { focusRequester.requestFocus() }
    }
    fun dismiss() {
        controller.dismiss({
            focusManager.clearFocus()
            keyboard?.hide()
            page.replyEditing = false
        }, { page.replyTarget.clear() })
    }

    /**
     * 返回键和点空白处共用：输入栏整体退出。草稿按回复对象存起来（下次回复同一个人会带回来），
     * 图片/表情面板/编辑态清掉，不留下「收起来但还能继续输入」的半开状态。
     */
    fun exit() {
        controller.saveAndDetach()
        focusManager.clearFocus()
        keyboard?.hide()
        page.replyEditing = false
        page.replyTarget.clear()
    }
    fun send() {
        val request = controller.beginSend { page.commentSender } ?: return
        scope.launch { if (controller.send(request)) dismiss() }
    }
}
