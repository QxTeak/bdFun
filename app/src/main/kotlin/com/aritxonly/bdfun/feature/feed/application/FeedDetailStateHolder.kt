package com.aritxonly.bdfun.feature.feed.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.FeedDetailRepository
import com.aritxonly.bdfun.core.model.feed.FeedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class FeedDetailUiState(
    val detail: FeedItem,
    val liked: Boolean = detail.liked,
    val likeCount: Int = detail.like,
)

internal class FeedDetailStateHolder(private val repository: FeedDetailRepository, initial: FeedItem) {
    private val mutable = MutableStateFlow(FeedDetailUiState(initial))
    val state = mutable.asStateFlow()
    private val id = initial.id
    private var closed = false
    private var revision = 0

    fun close() { closed = true; revision++ }

    suspend fun load() {
        if (closed) return
        val ticket = ++revision
        val result = repository.detail(id)
        if (closed || ticket != revision) return
        if (result is Outcome.Success) result.value?.let { mutable.value = FeedDetailUiState(it) }
    }

    suspend fun loadIpLocation() {
        if (closed) return
        val detail = mutable.value.detail
        if (detail.ipLocation.isNotBlank() || detail.mid <= 0L) return
        val result = repository.userIpLocation(detail.mid)
        if (closed) return
        if (result is Outcome.Success && result.value.isNotBlank()) mutable.update { current ->
            if (current.detail.mid == detail.mid && current.detail.ipLocation.isBlank())
                current.copy(
                    detail = current.detail.copy(
                        author = current.detail.author.copy(
                            ipLocation = result.value,
                        ),
                    ),
                ) else current
        }
    }

    /** 点击时立即更新，发送仍由原 UI 协程执行；保留连续点击的并发语义。 */
    fun beginLike(): Boolean? {
        if (closed) return null
        val current = mutable.value
        val next = !current.liked
        mutable.value = current.copy(liked = next, likeCount = current.likeCount + if (next) 1 else -1)
        return next
    }

    suspend fun commitLike(next: Boolean): String? {
        if (closed) return null
        val result = repository.like(mutable.value.detail.id, next)
        if (closed) return null
        if (result is Outcome.Success && result.value) return if (next) "点赞成功" else "已取消赞"
        // 原入口按当前计数加反向增量，不用快照覆盖期间发生的其它点击或详情补全。
        mutable.update { it.copy(liked = !next, likeCount = it.likeCount + if (!next) 1 else -1) }
        return "点赞失败"
    }
}
