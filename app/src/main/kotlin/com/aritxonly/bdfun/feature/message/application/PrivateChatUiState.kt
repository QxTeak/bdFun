package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.model.message.ChatMessage

internal data class ChatComposerUiState(
    val draft: String = "", val pendingImage: String? = null, val showEmoji: Boolean = false,
    val sending: Boolean = false, val uploading: Boolean = false,
)
internal data class PrivateChatUiState(
    val messages: List<ChatMessage> = emptyList(), val loading: Boolean = true, val loadError: Boolean = false,
    val historyCursor: Long? = null, val historyLoading: Boolean = false, val historyError: String? = null,
    val loadErrorText: String? = null,
    val emotes: Map<String, String> = emptyMap(), val myAvatar: String = "", val composer: ChatComposerUiState = ChatComposerUiState(),
)
