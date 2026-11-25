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
    // B 站给楼中楼回复自动拼「 回复 @某人 :」。折叠预览里把它读成一句话「某某回复 @某人：…」，
    // 所以这种正文前面不再补名字后的冒号；直接回复楼主时正文没有这段前缀，仍保留「某某：」的写法。
    val replyPrefix = remember(reply.message) { reply.message.trimStart().startsWith("回复 @") }
    EmoteText(
        content = EmoteTextContent(
            text = reply.message,
            prefix = reply.uname,
            extraEmotes = reply.emotes,
            mentions = remember(reply.mentions) {
            reply.mentions.associate { it.name to it.mid }
        },
            // 被 @ 的人是大会员时，引用目标用主题色（和大会员名字一致），不要灰/黑。
            vipMentions = remember(reply.mentions) {
            reply.mentions.filter { it.isVip }.map { it.name }.toSet()
        },
        ),
        style = EmoteTextStyle(
            color = colors.onBackground,
            // 折叠回复的用户名：大会员保持主题色，普通用户和主评论区用户名同一个灰色。
            prefixColor = if (reply.isVip) colors.primary else colors.onBackgroundVariant,
            fontSize = CommentTypography.previewBodySize,
            lineHeight = CommentTypography.previewLineHeight,
            prefixFontSize = CommentTypography.previewNameSize,
            fontWeight = FontWeight.Normal,
            // 正文里的 @ 仍用「我的」页快捷入口那支蓝色；只有「回复 @某人」的引用目标改成普通用户名的灰。
            mentionColor = MineBlue,
            // 只有正文确实是「 回复 @某人 :」时，开头的 @ 才是引用目标；直接回复楼主的正文不算。
            replyTargetMentionColor = if (replyPrefix) colors.onBackgroundVariant else null,
            vipMentionColor = colors.primary,
            trailingWidth = if (likeCount > 0) 56.sp else 26.sp,
        ),
        modifier = Modifier.weight(1f),
        decorations = EmoteTextDecorations(
            prefixColon = !replyPrefix,
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
