package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.MessageFeedRepository
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.model.feed.FeedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MessageFeedKind { Reply, Like, Fans, System }

internal data class MessageFeedUiState(
    val itemsByTab: Map<Int, List<MessageItem>> = emptyMap(),
    val loadingByTab: Map<Int, Boolean> = emptyMap(),
    val errorByTab: Map<Int, String?> = emptyMap(),
    val nextByTab: Map<Int, MessageCursor?> = emptyMap(),
    val retryMoreByTab: Map<Int, Boolean> = emptyMap(),
    val fanPages: Map<Int, Int> = emptyMap(),
    val followedOverride: Map<Long, Boolean> = emptyMap(),
    val followBusy: Set<Long> = emptySet(),
    val busy: Boolean = false,
)

internal class MessageFeedStateHolder(private val repository: MessageFeedRepository, val kind: MessageFeedKind,
    private val isCurrentOwner: () -> Boolean = { true }) {
    private val mutable = MutableStateFlow(MessageFeedUiState())
    val state = mutable.asStateFlow()
    private var closed = false
    private fun ownerIsCurrent() = !closed && isCurrentOwner()
    fun close() {
        closed = true
        mutable.update { it.copy(loadingByTab = emptyMap(), busy = false, followBusy = emptySet()) }
    }

    suspend fun loadFooter(page: Int) {
        val key = if (page == 1) 0 else page
        val current = mutable.value
        loadPage(page, if (current.errorByTab[key] != null) current.retryMoreByTab[key] == true
            else current.nextByTab[key] != null && current.itemsByTab.containsKey(key))
    }

    fun feedType(page: Int) = when (kind) {
        MessageFeedKind.Reply -> if (page == 2) 1 else 0
        MessageFeedKind.Like -> 2
        else -> 3
    }

    suspend fun loadPage(page: Int, more: Boolean = false) {
        val key = if (page == 1) 0 else page
        val initial = mutable.value
        if (!ownerIsCurrent() || initial.loadingByTab[key] == true || initial.busy || (more && initial.nextByTab[key] == null)) return
        mutable.update { it.copy(loadingByTab = it.loadingByTab + (key to true),
            errorByTab = it.errorByTab + (key to null), retryMoreByTab = it.retryMoreByTab + (key to more)) }
        try {
            val pn = if (more) (initial.fanPages[key] ?: 1) + 1 else 1
            val result = if (kind == MessageFeedKind.Fans) {
                when (val fans = repository.followers(pn)) {
                    is Outcome.Success -> Outcome.Success(MessageFeedPage(fans.value,
                        if (fans.value.size >= 20) MessageCursor(pn.toLong()) else null))
                    is Outcome.Failure -> fans
                }
            } else repository.page(feedType(key), if (more) initial.nextByTab[key] else null)
            if (!ownerIsCurrent()) return
            when (result) {
                is Outcome.Success -> mutable.update { it.copy(
                    itemsByTab = it.itemsByTab + (key to ((if (more) it.itemsByTab[key].orEmpty() else emptyList())
                        + result.value.items).distinctBy { item -> item.id }),
                    nextByTab = it.nextByTab + (key to result.value.next),
                    fanPages = it.fanPages + (key to pn),
                ) }
                is Outcome.Failure -> mutable.update { it.copy(errorByTab = it.errorByTab + (key to result.error.messageOr("消息加载失败"))) }
            }
        } finally { mutable.update { if (!ownerIsCurrent()) it else it.copy(loadingByTab = it.loadingByTab + (key to false)) } }
    }

    suspend fun toggleFollow(item: MessageItem): String? {
        val mid = item.userMid
        val initial = mutable.value
        if (!ownerIsCurrent() || mid <= 0 || mid in initial.followBusy) return null
        val target = !(initial.followedOverride[mid] ?: item.followed)
        mutable.update { it.copy(followedOverride = it.followedOverride + (mid to target), followBusy = it.followBusy + mid) }
        var committed = false
        try {
            return when (val result = repository.follow(mid, target)) {
                is Outcome.Success -> {
                    if (!ownerIsCurrent()) return null
                    committed = result.value
                    if (committed) { if (target) "已回关" else "已取消关注" } else "关注操作失败"
                }
                is Outcome.Failure -> if (!ownerIsCurrent()) null else result.error.messageOr("操作失败")
            }
        } finally {
            mutable.update { if (!ownerIsCurrent()) it else it.copy(followBusy = it.followBusy - mid,
                followedOverride = if (committed) it.followedOverride else it.followedOverride + (mid to !target)) }
        }
    }

    suspend fun dynamic(id: String): Outcome<FeedItem?> {
        if (!ownerIsCurrent()) throw kotlinx.coroutines.CancellationException("消息页面已关闭")
        val result = repository.dynamic(id)
        if (!ownerIsCurrent()) throw kotlinx.coroutines.CancellationException("消息页面已关闭")
        return result
    }

    suspend fun mutate(page: Int, item: MessageItem, delete: Boolean): Outcome<Unit>? {
        if (!ownerIsCurrent() || mutable.value.busy || mutable.value.loadingByTab[page] == true) return null
        mutable.update { it.copy(busy = true) }
        try {
            val result = if (delete) repository.delete(if (kind == MessageFeedKind.System) null else feedType(page), item.id)
                else repository.notice(item.id, item.noticeState != 0)
            if (!ownerIsCurrent()) return null
            if (result is Outcome.Success) mutable.update { it.copy(itemsByTab = it.itemsByTab + (page to
                it.itemsByTab[page].orEmpty().mapNotNull { current ->
                    if (current.id != item.id) current else if (delete) null
                    else current.copy(notice = current.notice.copy(noticeState = if (item.noticeState == 0) 1 else 0))
                })) }
            return result
        } finally { mutable.update { it.copy(busy = false) } }
    }
}
