package com.aritxonly.bdfun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.message.MessageSession
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun androidx.compose.foundation.lazy.LazyListScope.privateChatMessageItems(
    content: com.aritxonly.bdfun.feature.message.application.PrivateChatUiState, session: MessageSession,
) {
    itemsIndexed(content.messages.asReversed(), key = { _, message -> message.id }) { reversedIndex, message ->
        val originalIndex = content.messages.lastIndex - reversedIndex
        val previous = content.messages.getOrNull(originalIndex - 1)
        val showTime = previous == null ||
            message.timestamp - previous.timestamp > 600L ||
            !sameChatDay(previous.timestamp, message.timestamp)
        if (showTime) {
            Text(
                text = formatChatTime(message.timestamp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
        ChatBubble(
            message = message,
            otherAvatar = session.avatar,
            myAvatar = content.myAvatar, emotes = content.emotes, talkerId = session.talkerId,
        )
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.privateChatHistoryItem(
    content: com.aritxonly.bdfun.feature.message.application.PrivateChatUiState, onHistory: () -> Unit,
) {
    item(key = "history") {
        Text(if (content.historyLoading) "加载中…" else content.historyError?.let { "$it · 点击重试" }
            ?: if (content.historyCursor != null) "加载更早的消息" else "没有更早的消息了",
            color = MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable(enabled = !content.historyLoading && content.historyCursor != null) { onHistory() }
                .padding(vertical = 16.dp))
    }
}
