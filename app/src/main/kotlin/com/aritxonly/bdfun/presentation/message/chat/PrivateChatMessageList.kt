package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.message.MessageSession

@Composable
internal fun PrivateChatMessageList(
    content: com.aritxonly.bdfun.feature.message.application.PrivateChatUiState, session: MessageSession,
    listState: LazyListState, onHistory: () -> Unit,
) {
    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp + com.aritxonly.bdfun.ui.rememberStatusBarTop())
            .imePadding(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 12.dp,
            bottom = 118.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        privateChatMessageItems(content, session)
        privateChatHistoryItem(content, onHistory)

    }
}
