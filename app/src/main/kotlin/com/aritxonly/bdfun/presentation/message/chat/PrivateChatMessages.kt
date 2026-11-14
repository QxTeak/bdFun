package com.aritxonly.bdfun

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.core.model.message.MessageSession

@Composable
internal fun PrivateChatMessages(
    content: com.aritxonly.bdfun.feature.message.application.PrivateChatUiState,
    session: MessageSession, listState: LazyListState, onRetry: () -> Unit, onHistory: () -> Unit,
) {
    if (content.loading) PrivateChatLoading()
    else if (content.loadError) PrivateChatLoadError(content.loadErrorText, onRetry)
    else PrivateChatMessageList(content, session, listState, onHistory)
}
