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
    val extraEmotes: Map<String, String> = emptyMap(), val mentions: Map<String, Long> = emptyMap())
@androidx.compose.runtime.Immutable
internal data class EmoteTextStyle(val color: Color, val fontSize: TextUnit, val lineHeight: TextUnit,
    val prefixColor: Color = color, val prefixWeight: FontWeight = FontWeight.Normal,
    val maxLines: Int = Int.MAX_VALUE, val trailingWidth: TextUnit = 40.sp,
    val fontWeight: FontWeight = FontWeight.Normal, val mentionColor: Color = color,
    // 用户名字号可以和正文不同（例如折叠回复里名字比正文大 1sp）。
    val prefixFontSize: TextUnit = TextUnit.Unspecified)
@androidx.compose.runtime.Immutable
internal data class EmoteTextDecorations(val prefixBadge: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null)
internal class RichTextActions(val onVideo: (String) -> Unit, val onUri: (String) -> Unit, val onMentionClick: ((Long) -> Unit)?)
internal class RichTextRenderContext(val content: EmoteTextContent, val style: EmoteTextStyle,
    val decorations: EmoteTextDecorations, val actions: RichTextActions, val linkColor: Color,
    val biliTitles: Map<String, ResolvedVideoLink>, val analysis: RichTextAnalysis)
