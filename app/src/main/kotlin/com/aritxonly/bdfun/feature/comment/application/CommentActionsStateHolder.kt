package com.aritxonly.bdfun.feature.comment.application

import com.aritxonly.bdfun.core.application.CommentLike
import com.aritxonly.bdfun.core.application.CommentLikeStore
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.comment.CommentItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class CommentActionState(val sending: Boolean = false, val pending: Set<Long> = emptySet(), val sendError: AppError? = null)

internal class CommentActionsStateHolder(
    private val repository: CommentContentRepository,
    private val query: CommentQuery,
    private val likes: CommentLikeStore,
    private val sendTimeoutMs: Long = 20_000,
) {
    private val mutableState = MutableStateFlow(CommentActionState())
    val state = mutableState.asStateFlow()
    private val owner = likes.generation
    private var closed = false
    private val rollback = mutableMapOf<Long, CommentLike>()
    val isActive get() = !closed && owner == likes.generation

    fun close() {
        if (closed) return
        closed = true
        // 先移除回退记录，取消协程晚恢复时不能再覆盖新页面的快照。
        rollback.keys.toList().forEach(::restoreLike)
        mutableState.value = CommentActionState()
    }

    private fun restoreLike(rpid: Long) {
        val before = rollback.remove(rpid) ?: return
        likes.put(rpid, before.liked, before.count, owner)
    }

    suspend fun toggleLike(item: CommentItem): String? {
        if (!isActive || item.rpid in mutableState.value.pending) return null
        val token = owner
        val before = likes.state.value[item.rpid]
        val current = before?.liked ?: item.liked
        val count = before?.count ?: item.like
        val next = !current
        rollback[item.rpid] = CommentLike(current, count)
        mutableState.update { it.copy(pending = it.pending + item.rpid) }
        likes.put(item.rpid, next, (count + if (next) 1 else -1).coerceAtLeast(0), token)
        try {
            val result = repository.like(query, item.rpid, next)
            currentCoroutineContext().ensureActive()
            if (!isActive) return null
            return if (result is Outcome.Success) {
                if (next) "点赞成功" else "已取消点赞"
            } else {
                restoreLike(item.rpid)
                "点赞失败"
            }
        } catch (cancelled: CancellationException) {
            restoreLike(item.rpid)
            throw cancelled
        } finally {
            rollback.remove(item.rpid)
            if (!closed) mutableState.update { it.copy(pending = it.pending - item.rpid) }
        }
    }

    suspend fun post(text: String, root: Long, parent: Long, pictures: String? = null): Long? {
        if (!isActive || mutableState.value.sending) return null
        mutableState.update { it.copy(sending = true, sendError = null) }
        try {
            val result = kotlinx.coroutines.withTimeoutOrNull(sendTimeoutMs) {
                repository.post(CommentPost(query, text, root, parent, pictures))
            } ?: Outcome.Failure(AppError.Network("发送超时，请检查评论是否已发出再重试"))
            currentCoroutineContext().ensureActive()
            if (!isActive) return null
            if (result is Outcome.Failure) mutableState.update { it.copy(sendError = result.error) }
            return (result as? Outcome.Success)?.value
        } finally { if (!closed) mutableState.update { it.copy(sending = false) } }
    }

    suspend fun report(rpid: Long): Boolean = perform { repository.report(query, rpid) }
    suspend fun delete(rpid: Long): Boolean = perform { repository.delete(query, rpid) }
    suspend fun block(mid: Long): Boolean = mid > 0 && perform { repository.block(mid) }

    private suspend fun perform(action: suspend () -> Outcome<Unit>): Boolean {
        if (!isActive) return false
        val result = action()
        currentCoroutineContext().ensureActive()
        return isActive && result is Outcome.Success
    }
}
