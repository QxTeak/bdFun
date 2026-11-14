package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.domain.PrivateChatRepository
import com.aritxonly.bdfun.core.model.message.ChatMessagePage
import com.aritxonly.bdfun.core.model.message.MessageSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 单会话、单账号实例；UI 只提供是否在底部及滚动动作，不进入请求和发送事务。 */
internal class PrivateChatStateHolder(
    private val repository: PrivateChatRepository, private val session: MessageSession,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val isCurrentOwner: () -> Boolean = { true },
) {
    private val mutable = MutableStateFlow(PrivateChatUiState())
    val state = mutable.asStateFlow()
    private var generation = 0
    private var loadRevision = 0
    private var closed = false
    private fun ownerIsCurrent() = !closed && isCurrentOwner()

    fun setDraft(value: String) = composer { it.copy(draft = value) }
    fun setImage(reference: String?) = composer { it.copy(pendingImage = reference) }
    fun toggleEmoji() = composer { it.copy(showEmoji = !it.showEmoji) }
    fun appendEmoji(value: String) = composer { it.copy(draft = it.draft + value) }
    private fun composer(update: (ChatComposerUiState) -> ChatComposerUiState) {
        if (ownerIsCurrent()) mutable.update { it.copy(composer = update(it.composer)) }
    }

    fun close() {
        closed = true; generation++; loadRevision++
        mutable.update { it.copy(loading = false, historyLoading = false,
            composer = it.composer.copy(sending = false, uploading = false)) }
    }

    suspend fun load() {
        if (!ownerIsCurrent()) return
        val token = generation
        val revision = ++loadRevision
        mutable.update { it.copy(loading = true, loadError = false, loadErrorText = null) }
        try {
            var loaded: ChatMessagePage? = null
            var loadFailure: String? = null
            for (attempt in 0 until 3) {
                ensureCurrent(token)
                if (revision != loadRevision) throw CancellationException("已开始新的消息加载")
                val result = repository.page(session)
                ensureCurrent(token)
                if (revision != loadRevision) throw CancellationException("已开始新的消息加载")
                if (result is Outcome.Success) { loaded = result.value; break }
                if (result is Outcome.Failure) loadFailure = result.error.chatText("消息加载失败")
                if (attempt < 2) wait(400)
            }
            mutable.update { if (loaded != null) it.copy(messages = loaded.items, emotes = loaded.emotes,
                historyCursor = loaded.next, loading = false, loadErrorText = null)
                else it.copy(loadError = true, loadErrorText = loadFailure, loading = false) }
            if (loaded != null) acknowledgeLatest(token)
            val avatar = repository.myAvatar()
            ensureCurrent(token)
            if (revision == loadRevision) mutable.update { it.copy(myAvatar = (avatar as? Outcome.Success)?.value.orEmpty()) }
        } finally {
            if (token == generation && revision == loadRevision) mutable.update { it.copy(loading = false) }
        }
    }

    suspend fun loadHistory() {
        val initial = mutable.value
        val cursor = initial.historyCursor ?: return
        if (!ownerIsCurrent() || initial.historyLoading) return
        val token = generation
        mutable.update { it.copy(historyLoading = true, historyError = null) }
        try {
            val result = repository.page(session, cursor)
            ensureCurrent(token)
            mutable.update { current -> when (result) {
                is Outcome.Success -> current.copy(messages = (result.value.items + current.messages)
                    .distinctBy { it.id }.sortedBy { it.sequence }, historyCursor = result.value.next, emotes = current.emotes + result.value.emotes)
                is Outcome.Failure -> current.copy(historyError = result.error.chatText("历史消息加载失败"))
            } }
        } finally {
            if (token == generation) mutable.update { it.copy(historyLoading = false) }
        }
    }

    suspend fun refreshLatest(shouldAcknowledge: () -> Boolean): Outcome<Unit> {
        val token = generation
        ensureCurrent(token)
        val result = repository.page(session)
        ensureCurrent(token)
        if (result is Outcome.Failure) return result
        val page = (result as Outcome.Success).value
        val updates = page.items.associateBy { it.id }
        mutable.update { it.copy(emotes = it.emotes + page.emotes, messages = (it.messages.filterNot { item -> item.id in updates } + page.items)
            .sortedBy { item -> item.sequence }) }
        return if (shouldAcknowledge()) acknowledgeLatest(token) else Outcome.Success(Unit)
    }

    suspend fun pollLatest(shouldAcknowledge: () -> Boolean) {
        val current = mutable.value
        if (!ownerIsCurrent() || current.loading || current.composer.sending || current.loadError) return
        refreshLatest(shouldAcknowledge)
    }

    suspend fun send(shouldAcknowledge: () -> Boolean, scrollToLatest: suspend () -> Unit): String? {
        val initial = mutable.value.composer
        if (!ownerIsCurrent() || initial.sending || initial.uploading) return null
        val value = initial.draft.trim()
        val image = initial.pendingImage
        if (value.isBlank() && image == null) return null
        val token = generation
        composer { it.copy(sending = true) }
        try {
            if (image != null) sendImage(image, token)?.let { return it }
            if (value.isNotBlank()) sendText(value, token)?.let { return it }
            composer { it.copy(showEmoji = false) }
            if (refreshLatest(shouldAcknowledge) is Outcome.Failure) return "已发送，消息列表稍后更新"
            try { scrollToLatest() }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { return "已发送，消息列表稍后更新" }
            return null
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            return failure.message ?: "发送失败，请稍后重试"
        } finally {
            if (token == generation) composer { it.copy(sending = false, uploading = false) }
        }
    }

    suspend fun withdraw(message: com.aritxonly.bdfun.core.model.message.ChatMessage): String? {
        if (!ownerIsCurrent() || mutable.value.composer.sending || !message.isMine || message.withdrawn) return null
        val token = generation
        composer { it.copy(sending = true) }
        try {
            val result = repository.withdraw(session, message)
            ensureCurrent(token)
            return when (result) {
                is Outcome.Success -> {
                    mutable.update { it.copy(messages = it.messages.map { current ->
                        if (current.id == message.id) current.copy(text = "消息已撤回", imageUrl = "", card = null,
                            sharedVideo = null, withdrawn = true) else current }) }; null
                }
                is Outcome.Failure -> result.error.chatText("撤回失败")
            }
        } finally { if (token == generation) composer { it.copy(sending = false) } }
    }
    suspend fun report(message: com.aritxonly.bdfun.core.model.message.ChatMessage, reason: Int, description: String): String? {
        if (!ownerIsCurrent()) return null
        val token = generation
        val result = repository.report(message, reason, description)
        ensureCurrent(token)
        return when (result) { is Outcome.Success -> "举报成功"; is Outcome.Failure -> result.error.chatText("举报失败") }
    }

    private suspend fun sendImage(reference: String, token: Int): String? {
        composer { it.copy(uploading = true) }
        val uploaded = repository.uploadImage(reference)
        ensureCurrent(token)
        if (uploaded is Outcome.Failure) return uploaded.error.chatText("发送失败，请稍后重试")
        val result = repository.sendImage(session, (uploaded as Outcome.Success).value)
        ensureCurrent(token)
        if (result is Outcome.Failure) return result.error.chatText("发送失败，请稍后重试")
        if (!(result as Outcome.Success).value) return "图片发送失败"
        // 图片一旦发送成功就清掉，即使接下来的文字发送失败也不能重发图片。
        composer { it.copy(pendingImage = null, uploading = false) }
        return null
    }

    private suspend fun sendText(value: String, token: Int): String? {
        val result = repository.sendText(session, value)
        ensureCurrent(token)
        if (result is Outcome.Failure) return result.error.chatText("发送失败，请稍后重试")
        if (!(result as Outcome.Success).value) return "文字发送失败，请重试"
        composer { if (it.draft.trim() == value) it.copy(draft = "") else it }
        return null
    }

    private suspend fun acknowledgeLatest(token: Int): Outcome<Unit> {
        val messages = mutable.value.messages
        if (messages.size == 1 && messages[0].type == 18 && messages[0].source == 18) return Outcome.Success(Unit)
        val sequence = messages.maxOfOrNull { it.sequence } ?: 0L
        if (sequence <= 0L) return Outcome.Success(Unit)
        val result = repository.acknowledge(session, sequence)
        ensureCurrent(token)
        return result
    }

    private fun ensureCurrent(token: Int) {
        if (!ownerIsCurrent() || token != generation) throw CancellationException("私信页面作用域已结束")
    }
}

private fun AppError.chatText(fallback: String): String = when (this) {
    is AppError.Unknown -> message
    else -> messageOr(fallback)
}
