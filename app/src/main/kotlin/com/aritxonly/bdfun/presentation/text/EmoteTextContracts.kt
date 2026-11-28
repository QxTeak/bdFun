package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.application.*
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink

@androidx.compose.runtime.Immutable
internal data class EmoteTextContent(val text: String, val prefix: String? = null,
    val extraEmotes: Map<String, String> = emptyMap(), val mentions: Map<String, Long> = emptyMap(),
    /** 被 @ 的大会员名字：折叠回复里的引用目标要跟着用主题色，而不是灰色。 */
    val vipMentions: Set<String> = emptySet())
@androidx.compose.runtime.Immutable
internal data class EmoteTextStyle(val color: Color, val fontSize: TextUnit, val lineHeight: TextUnit,
    val prefixColor: Color = color, val prefixWeight: FontWeight = FontWeight.Normal,
    val maxLines: Int = Int.MAX_VALUE, val trailingWidth: TextUnit = 40.sp,
    val fontWeight: FontWeight = FontWeight.Normal,
    // 正文里的 @提及统一用「我的」页快捷图标那支蓝色，所有调用方默认生效。
    val mentionColor: Color = MineBlue,
    // 折叠回复正文开头的「回复 @某人」：这个 @ 要和普通用户名同一个灰，正文里其它 @ 仍是蓝的。
    val replyTargetMentionColor: Color? = null,
    // 被 @ 的人是大会员时，引用目标改用主题色（不要灰/黑）。
    val vipMentionColor: Color? = null,
    // 用户名字号可以和正文不同（例如折叠回复里名字比正文大 1sp）。
    val prefixFontSize: TextUnit = TextUnit.Unspecified)
@androidx.compose.runtime.Immutable
internal data class EmoteTextDecorations(val prefixBadge: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    // 折叠回复正文本身就是「 回复 @某人 : …」，再补一个冒号会变成「某某：回复 @某人」，所以这里可关掉。
    val prefixColon: Boolean = true)
internal class RichTextActions(
    val onVideo: (String) -> Unit, val onUri: (String) -> Unit, val onMentionClick: ((Long) -> Unit)?,
    /** 点评论里的时间点：秒数；null 表示当前页面没有播放器，不标色也不可点。 */
    val onSeek: ((Int) -> Unit)? = null,
)
internal class RichTextRenderContext(val content: EmoteTextContent, val style: EmoteTextStyle,
    val decorations: EmoteTextDecorations, val actions: RichTextActions, val linkColor: Color,
    val biliTitles: Map<String, ResolvedVideoLink>, val analysis: RichTextAnalysis)
