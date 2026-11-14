package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.message.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class MessageSessionsUiState(
    val sessions: List<MessageSession> = emptyList(),
    val unread: Map<String, Int> = emptyMap(),
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val next: Long? = null,
    val retryMore: Boolean = false,
    val revision: Int = 0,
    val inboxCursor: String? = null,
    val menu: List<MessageMenuItem> = emptyList(),
    val outside: List<MessageMenuItem> = emptyList(),
)

/** 会话写操作和分页共用互斥状态，旧的姓名补全不能覆盖写操作后的数据。 */
internal class MessageSessionsStateHolder(private val repository: MessageSessionsRepository,
    private val isCurrentOwner: () -> Boolean = { true },
    private val pageType: Int = 1) {
    private val mutable = MutableStateFlow(MessageSessionsUiState())
    val state = mutable.asStateFlow()

    private var closed = false
    private fun ownerIsCurrent() = !closed && isCurrentOwner()
    fun close() { closed = true; reset() }

    suspend fun loadFooter() {
        val current = mutable.value
        load(if (current.error != null) current.retryMore else current.inboxCursor != null && current.sessions.isNotEmpty())
    }

    fun reset() { mutable.value = MessageSessionsUiState(revision = mutable.value.revision + 1) }

    suspend fun load(more: Boolean = false) {
        val initial = mutable.value
        if (!ownerIsCurrent() || initial.loading || initial.busy) return
        val cursor = if (more) initial.inboxCursor ?: return else null
        val version = initial.revision + 1
        mutable.value = initial.copy(loading = true, retryMore = more, error = null, revision = version)
        val names = java.util.concurrent.ConcurrentHashMap<String, MessageSession>()
        try {
            val result = repository.inbox(cursor, pageType) { enriched ->
                names.putAll(enriched.associateBy { it.id })
                mutable.update { current ->
                    if (!ownerIsCurrent() || current.revision != version) current else current.copy(sessions = enrich(current.sessions, names))
                }
            }
            if (!ownerIsCurrent() || mutable.value.revision != version) return
            when (result) {
                is Outcome.Success -> mutable.update { current -> current.copy(
                    sessions = enrich(((if (more) current.sessions else emptyList()) + result.value.sessions)
                        .distinctBy { it.id }.sortedByDescending { it.pinned }, names), next = result.value.cursor?.toLongOrNull(), inboxCursor = result.value.cursor,
                    menu = if (more) current.menu else result.value.menu,
                    outside = if (more) current.outside else result.value.outside,
                ) }
                is Outcome.Failure -> mutable.update { it.copy(error = result.error.messageOr("消息加载失败")) }
            }
            when (val unread = repository.unread()) {
                is Outcome.Success -> if (ownerIsCurrent() && mutable.value.revision == version) mutable.update { it.copy(unread = unread.value) }
                is Outcome.Failure -> Unit // 未读计数失败不遮挡已成功加载的会话。
            }
        } finally {
            mutable.update { if (it.revision == version) it.copy(loading = false) else it }
        }
    }

    suspend fun clearUnread(): String? {
        val current = mutable.value
        if (!ownerIsCurrent() || current.busy || current.loading) return null
        val version = current.revision + 1
        mutable.value = current.copy(busy = true, revision = version)
        val message = try {
            when (val result = repository.readInbox(pageType, current.sessions)) {
                is Outcome.Success -> if (result.value) "已全部标为已读" else "部分消息尚未标为已读，请重试"
                is Outcome.Failure -> result.error.messageOr("标记已读失败")
            }
        } finally { mutable.update { if (it.revision == version) it.copy(busy = false) else it } }
        if (!ownerIsCurrent() || mutable.value.revision != version) return null
        load()
        return if (!ownerIsCurrent()) null else message
    }

    suspend fun clearList(): String? {
        val current = mutable.value
        if (!ownerIsCurrent() || current.busy || current.loading) return null
        val revision = current.revision + 1
        mutable.value = current.copy(busy = true, revision = revision)
        try {
            val result = repository.clearInbox(pageType)
            if (!ownerIsCurrent() || mutable.value.revision != revision) return null
            return when (result) {
                is Outcome.Success -> { mutable.update { it.copy(sessions = emptyList(), inboxCursor = null, next = null) }; "已清空列表" }
                is Outcome.Failure -> result.error.messageOr("清空失败")
            }
        } finally { mutable.update { if (it.revision == revision) it.copy(busy = false) else it } }
    }

    suspend fun mutate(target: MessageSession, action: SessionAction): Outcome<Unit>? {
        val current = mutable.value
        if (!ownerIsCurrent() || current.busy || current.loading) return null
        val version = current.revision + 1
        mutable.value = current.copy(busy = true, revision = version)
        try {
            val result = repository.mutate(target, action)
            if (!ownerIsCurrent() || mutable.value.revision != version) return null
            if (result is Outcome.Success) mutable.update { it.copy(
                sessions = it.sessions.mapNotNull { item ->
                if (item.id != target.id) item else when (action) {
                    SessionAction.Delete -> null
                    SessionAction.Pin -> item.copy(
                        preferences = item.preferences.copy(
                            pinned = !target.pinned,
                        ),
                    )
                    SessionAction.Mute -> item.copy(
                        preferences = item.preferences.copy(
                            muted = !target.muted,
                        ),
                    )
                    SessionAction.Read -> item.copy(unread = 0, preferences = item.preferences.copy(unreadDot = false))
                }
            }.sortedByDescending { it.pinned },
            ) }
            return result
        } finally { mutable.update { if (it.revision == version) it.copy(busy = false) else it } }
    }

    private fun enrich(items: List<MessageSession>, names: Map<String, MessageSession>) = items.map { item ->
        names[item.id]?.let { item.copy(name = it.name, avatar = it.avatar) } ?: item
    }
}

internal fun MessageSessionsUiState.visibleSessions(followingOnly: Boolean): List<MessageSession> =
    if (followingOnly) sessions.filter { it.followed } else sessions

internal fun Map<String, Int>.replyAndMentionUnread(): Int =
    (get("reply") ?: 0) + (get("at") ?: 0)
