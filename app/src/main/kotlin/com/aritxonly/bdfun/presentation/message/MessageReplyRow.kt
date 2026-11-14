package com.aritxonly.bdfun.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.message.MessageItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ReplyRow(item: MessageItem, onClick: () -> Unit = {}, onMore: () -> Unit = {}) {
    val colors = MiuixTheme.colorScheme
    Row(Modifier.fillMaxWidth()
        .combinedClickable(onClick = onClick, onLongClick = onMore)
        .padding(16.dp)) {
        MessageActorAvatar(item)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            MessageCardHeader(item)
            if (item.content.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                EmoteText(content = EmoteTextContent(text = item.content),
                    style = EmoteTextStyle(color = colors.onBackground,
                        fontSize = 15.sp, lineHeight = 23.sp, maxLines = Int.MAX_VALUE))
            }
            // 原回复与根评论都放在正文下，避免右侧窄列挤压消息。
            if (item.quote.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                MessageOriginalText(item.quote)
            }
            if (item.subjectText.isNotBlank() && item.subjectText != item.quote) {
                Spacer(Modifier.height(4.dp))
                MessageOriginalText(item.subjectText)
            }
            Spacer(Modifier.height(8.dp))
            MessageRowFooter(item)
        }
    }
}
