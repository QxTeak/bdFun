package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.application.isBiliVideoTextToken
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.application.*
import com.aritxonly.teak.common.player.PlayGlyph

internal fun AnnotatedString.Builder.appendRichLink(runtime: RichTextRenderContext, inline: MutableMap<String, InlineTextContent>, hit: RichTextHit) {
    val url = hit.linkUrl.orEmpty()
    val linkColor = runtime.linkColor
    val isVideo = isBiliVideoTextToken(url)
    val label = if (isVideo) biliDisplayText(url, runtime.biliTitles) else url
    // 只有解析出标题时才加播放符号；裸 BV/av 保持原样，避免和正文里的其它符号混淆。
    val showPlayGlyph = isVideo && label != url
    val bvid = if (isVideo) {
        runtime.biliTitles[url]?.bvid
            ?: if (url.startsWith("BV", ignoreCase = true)) "BV" + url.substring(2) else null
    } else null
    val open: () -> Unit = {
        if (isVideo && bvid != null) runtime.actions.onVideo(bvid) else runtime.actions.onUri(url)
    }
    withLink(
        androidx.compose.ui.text.LinkAnnotation.Clickable(
            tag = "url-${hit.start}",
            styles = androidx.compose.ui.text.TextLinkStyles(
                style = SpanStyle(color = linkColor),
            ),
            linkInteractionListener = { open() },
        ),
    ) {
        if (showPlayGlyph) {
            appendVideoPlayGlyph(runtime, inline, hit.start, linkColor, open)
        }
        append(label)
    }
}

/**
 * 视频链接标题前的播放符号：直接复用播放器的播放图标，
 * 尺寸跟随当前文字字号，点击行为和标题本身一致。
 */
private fun AnnotatedString.Builder.appendVideoPlayGlyph(
    runtime: RichTextRenderContext,
    inline: MutableMap<String, InlineTextContent>,
    hitStart: Int,
    tint: androidx.compose.ui.graphics.Color,
    open: () -> Unit,
) {
    val fontSize = runtime.style.fontSize.let { if (it.isSp) it else 14.sp }
    val id = "video-play-$hitStart"
    inline[id] = InlineTextContent(
        Placeholder(width = fontSize, height = fontSize, placeholderVerticalAlign = PlaceholderVerticalAlign.Center),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val glyphSize = minOf(maxWidth, maxHeight) * 0.92f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = null, indication = null, onClick = open),
                contentAlignment = Alignment.Center,
            ) {
                PlayGlyph(TeakIcon.PlayerPlay, tint = tint, size = glyphSize)
            }
        }
    }
    appendInlineContent(id)
}

internal fun AnnotatedString.Builder.appendRichMention(runtime: RichTextRenderContext, inline: MutableMap<String, InlineTextContent>, hit: RichTextHit) {
    val text = runtime.content.text
    val mentionColor = runtime.style.mentionColor
    val onMentionClick = runtime.actions.onMentionClick
    val mid = hit.mentionMid
    val name = hit.mentionName.orEmpty()
    withLink(
        androidx.compose.ui.text.LinkAnnotation.Clickable(
            tag = "mention-${hit.start}",
            styles = androidx.compose.ui.text.TextLinkStyles(
                style = SpanStyle(
                    color = mentionColor,
                    fontWeight = FontWeight.Normal,
                ),
            ),
            linkInteractionListener = { onMentionClick?.invoke(mid) },
        ),
    ) {
        append("@$name")
    }

}
