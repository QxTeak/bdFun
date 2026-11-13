package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.RowScope.CommentPreviewText(
    reply: CommentItem, liked: Boolean, likeCount: Int, actions: CommentPreviewActions,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    EmoteText(
        content = EmoteTextContent(
            text = reply.message,
            prefix = reply.uname,
            extraEmotes = reply.emotes,
            mentions = remember(reply.mentions) {
            reply.mentions.associate { it.name to it.mid }
        },
        ),
        style = EmoteTextStyle(
            color = colors.onBackground,
            // 折叠回复：普通用户用淡一点的主题色；大会员用完整主题色。
            prefixColor = if (reply.isVip) {
            colors.primary
        } else {
            colors.primary.copy(alpha = 0.72f)
        },
            fontSize = CommentTypography.previewBodySize,
            lineHeight = CommentTypography.previewLineHeight,
            prefixFontSize = CommentTypography.previewNameSize,
            fontWeight = FontWeight.Normal,
            mentionColor = if (reply.isVip) {
            colors.primary
        } else {
            colors.primary.copy(alpha = 0.72f)
        },
            trailingWidth = if (likeCount > 0) 56.sp else 26.sp,
        ),
        modifier = Modifier.weight(1f),
        decorations = EmoteTextDecorations(
            prefixBadge = if (reply.isUp) {
            { UpBadge() }
        } else {
            null
        },
            trailingContent = {
            CommentPreviewLike(liked, likeCount) { actions.onPreviewLike(reply) }
        },
        ),
        onMentionClick = { mid ->
            if (mid > 0L) {
                navigateToSpace(context, mid)
            }
        },
    )
}
