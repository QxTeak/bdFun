package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.EmoteTextContent
import com.aritxonly.bdfun.ui.EmoteTextStyle
import com.aritxonly.bdfun.ui.EmoteTextDecorations

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.message.ChatMessage
import com.aritxonly.bdfun.ui.EmoteText
import com.aritxonly.bdfun.ui.rememberImageRequest
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ChatBubble(
    message: ChatMessage,
    otherAvatar: String,
    myAvatar: String,
    emotes: Map<String, String> = emptyMap(),
) {
    val colors = MiuixTheme.colorScheme
    val mine = message.isMine
    val onMenu = LocalChatMessageMenu.current
    val onImage = LocalChatImageOpen.current
    Row(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { onMenu(message) }),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!mine) {
            ChatAvatar(url = otherAvatar)
            Spacer(Modifier.width(7.dp))
        }
        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 272.dp),
        ) {
            if (message.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(message.imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(max = 220.dp).clickable { onImage(message.imageUrl) }
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceContainerHigh),
                )
            }
            val sharedVideo = message.sharedVideo
            if (sharedVideo != null) {
                ChatSharedVideoCard(info = sharedVideo)
            } else if (message.card != null) {
                ChatMessageCardContent(message.card!!)
            } else if (message.text.isNotBlank()) {
                EmoteText(
                    content = EmoteTextContent(
                        text = message.text, extraEmotes = emotes,
                    ),
                    style = EmoteTextStyle(
                        color = if (mine) colors.onPrimary else colors.onBackground,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (mine) colors.primary else colors.surfaceContainerHigh)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
        }
        if (mine) {
            Spacer(Modifier.width(7.dp))
            ChatAvatar(url = myAvatar, placeholder = "我")
        }
    }
}
