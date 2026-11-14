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
internal fun LikeRow(item: MessageItem, onClick: () -> Unit = {}, onMore: () -> Unit = {}) {
    val colors = MiuixTheme.colorScheme
    Row(Modifier.fillMaxWidth()
        .combinedClickable(onClick = onClick, onLongClick = onMore)
        .padding(16.dp)) {
        MessageActorAvatar(item)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            MessageCardHeader(item, aggregate = true)
            val description = item.title.ifBlank { item.subjectText }.ifBlank { item.source }.ifBlank { item.content }
            if (description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                EmoteText(content = EmoteTextContent(text = description),
                    style = EmoteTextStyle(color = colors.onBackgroundVariant,
                        fontSize = 14.sp, lineHeight = 21.sp, maxLines = 3))
            }
            Spacer(Modifier.height(8.dp))
            MessageRowFooter(item)
        }
    }
}
