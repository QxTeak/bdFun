package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentRowMessage(item: CommentItem) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val photo = LocalPhotoViewer.current
    EmoteText(
        content = EmoteTextContent(
            text = item.message,
            extraEmotes = item.emotes,
            // 正文里 @ 到的人，和折叠回复的人名同色，点名字进对方主页。
            mentions = remember(item.mentions) {
            item.mentions.associate { it.name to it.mid }
        },
        ),
        style = EmoteTextStyle(
            color = colors.onBackground,
            fontSize = CommentTypography.bodySize,
            lineHeight = CommentTypography.bodyLineHeight,
            fontWeight = FontWeight.Normal,
            mentionColor = colors.primary.copy(alpha = 0.72f),
        ),
        onMentionClick = { mid ->
            if (mid > 0L) {
                navigateToSpace(context, mid)
            }
        },
    )
    if (item.pictures.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        CommentPictures(item.pictures) { index, rect, radius ->
            photo.open(item.pictures, index, rect, radius)
        }
    }
}
