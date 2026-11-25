package com.aritxonly.bdfun.feature.comment.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.CommentImageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal typealias CommentSender = suspend (String, String?) -> Boolean
internal data class CommentSendRequest(val text: String, val sender: CommentSender)
internal data class CommentComposerUiState(
    val draft: String = "",
    val showEmoji: Boolean = false,
    val editing: Boolean = false,
    val sending: Boolean = false,
    val uploading: Boolean = false,
    val pendingPictures: String? = null,
    val pendingPreview: String? = null,
    val failed: Boolean = false,
)

/** 页面实例拥有草稿；附件与发送使用旧入口的读取/清空顺序，不改成乐观草稿合并。 */
internal class CommentComposerStateHolder(private val images: CommentImageRepository) {
    private val mutable = MutableStateFlow(CommentComposerUiState())
    val state = mutable.asStateFlow()
    private var closed = false

    fun close() {
        closed = true
        mutable.update { it.copy(sending = false, uploading = false) }
    }
    fun setDraft(value: String) { if (!closed) mutable.update { it.copy(draft = value) } }
    fun appendEmoji(value: String) { if (!closed) mutable.update { it.copy(draft = it.draft + value) } }
    fun setEditing(value: Boolean) { if (!closed) mutable.update { it.copy(editing = value) } }
    fun toggleEmoji() { if (!closed) mutable.update { it.copy(showEmoji = !it.showEmoji, editing = true) } }

    /**
     * 按「回复对象」记住草稿（issue #144）：同一个视频里给 A 打了一半的话，
     * 走开去回复 B、再回来找 A，输入框要把上次对 A 写的内容捞回来。
     */
    private val drafts = mutableMapOf<String, String>()
    private var activeKey: String? = null

    fun bindTarget(key: String) {
        if (closed || activeKey == key) return
        activeKey?.let { drafts[it] = mutable.value.draft }
        activeKey = key
        mutable.update { it.copy(draft = drafts[key].orEmpty()) }
    }

    /** 退出输入栏：先把当前草稿记在它对应的回复对象下面，再把界面状态清干净。 */
    fun saveAndDetach() {
        if (closed) return
        activeKey?.let { drafts[it] = mutable.value.draft }
        activeKey = null
        mutable.update {
            it.copy(
                draft = "", showEmoji = false, editing = false,
                pendingPictures = null, pendingPreview = null,
                uploading = false, failed = false,
            )
        }
    }

    /** 发送成功：这份草稿已经用掉，不能下次再被捞回来。 */
    private fun forgetActiveDraft() {
        activeKey?.let { drafts.remove(it) }
    }

    /**
     * 整体退出输入：草稿、图片、表情面板、编辑态一起清掉。
     * 退出后再打开不应该「记得」上一次的内容和回复对象（issue #144）。
     */
    fun reset() {
        if (closed) return
        saveAndDetach()
        drafts.clear()
    }

    fun dismiss(onDismissEditor: () -> Unit, onClearTarget: () -> Unit) {
        if (closed) return
        mutable.update { it.copy(showEmoji = false, editing = false) }
        onDismissEditor()
        // 焦点/IME 的同步回调可能刚提交文本，沿用原入口在收键盘后判断草稿的时机。
        if (mutable.value.draft.isBlank()) {
            onClearTarget()
            mutable.update { it.copy(pendingPictures = null, pendingPreview = null) }
        }
    }

    fun selectImage(reference: String): Boolean {
        if (closed) return false
        mutable.update { it.copy(pendingPreview = reference, uploading = true, failed = false) }
        return true
    }

    suspend fun upload(reference: String) {
        if (closed) return
        try {
            val result = images.upload(reference)
            currentCoroutineContext().ensureActive()
            if (closed) return
            mutable.update { it.copy(uploading = false) }
            val pictures = (result as? Outcome.Success)?.value
            if (pictures == null) mutable.update { it.copy(pendingPreview = null, failed = true) }
            else mutable.update { it.copy(pendingPictures = pictures) }
        } finally {
            if (!closed) mutable.update { it.copy(uploading = false) }
        }
    }

    fun beginSend(sender: () -> CommentSender?): CommentSendRequest? {
        val current = mutable.value
        val value = current.draft.trim()
        if (closed || (value.isEmpty() && current.pendingPictures == null) || current.sending || current.uploading) return null
        val selected = sender()
        if (selected == null) {
            mutable.update { it.copy(failed = true) }
            return null
        }
        mutable.update { it.copy(sending = true, failed = false) }
        return CommentSendRequest(value, selected)
    }

    suspend fun send(request: CommentSendRequest): Boolean {
        if (closed) return false
        try {
            val ok = try {
                request.sender(request.text, mutable.value.pendingPictures)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                false
            }
            currentCoroutineContext().ensureActive()
            if (closed) return false
            mutable.update { it.copy(sending = false) }
            if (ok) {
                forgetActiveDraft()
                mutable.update { it.copy(draft = "", pendingPictures = null, pendingPreview = null) }
            } else mutable.update { it.copy(failed = true) }
            return ok
        } finally {
            if (!closed) mutable.update { it.copy(sending = false) }
        }
    }
}
