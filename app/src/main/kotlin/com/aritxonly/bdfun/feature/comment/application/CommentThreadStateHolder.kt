package com.aritxonly.bdfun.feature.comment.application

import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class CommentThreadState(val extra: List<CommentItem> = emptyList(), val deleted: Set<Long> = emptySet())

/** 新发送回复和删除投影属于根楼作用域，切换根楼不沿用上一楼的本地条目。 */
internal class CommentThreadStateHolder(val actions: CommentActionsStateHolder, private val root: Long) {
    private val mutableState = MutableStateFlow(CommentThreadState())
    val state = mutableState.asStateFlow()
    private var closed = false
    val isActive get() = !closed && actions.isActive
    fun close() { closed = true }

    suspend fun send(text: String, parent: Long, profile: UserProfile?, clock: () -> Long): Long? {
        if (!isActive) return null
        val result = actions.post(text, root, parent) ?: return null
        currentCoroutineContext().ensureActive()
        if (!isActive) return null
        val nowMs = clock()
        val item = localCommentItem(
            rpid = result,
            text = text,
            profile = profile,
            oid = 0L,
            root = root,
            parent = parent,
            nowMs = nowMs,
        )
        // 按发送先后追加；展示层再把每条回复插到它回复的那条下面。
        mutableState.update { it.copy(extra = it.extra + item) }
        return result
    }
    suspend fun delete(rpid: Long): Boolean {
        if (!isActive) return false
        val ok = actions.delete(rpid)
        currentCoroutineContext().ensureActive()
        if (!isActive) return false
        if (ok) mutableState.update { it.copy(deleted = it.deleted + rpid) }
        return ok
    }
}
