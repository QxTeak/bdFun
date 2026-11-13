package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.takeOrElse
import com.aritxonly.bdfun.core.application.*

internal fun AnnotatedString.Builder.appendRichPrefix(runtime: RichTextRenderContext, inline: MutableMap<String, InlineTextContent>) {
    val prefix = runtime.content.prefix
    val prefixBadge = runtime.decorations.prefixBadge
    val prefixColor = runtime.style.prefixColor
    val prefixWeight = runtime.style.prefixWeight
    val fontSize = runtime.style.fontSize
    val prefixSize = runtime.style.prefixFontSize.takeOrElse { fontSize }
    if (!prefix.isNullOrBlank()) {
        withStyle(SpanStyle(color = prefixColor, fontWeight = prefixWeight, fontSize = prefixSize)) {
            append(prefix)
        }
        if (prefixBadge != null) {
            append(" ")
            appendInlineContent("prefixBadge", "UP")
            inline["prefixBadge"] = InlineTextContent(
                Placeholder(
                    width = prefixSize * 1.9f,
                    height = prefixSize * 1.35f,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) { _ ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    prefixBadge()
                }
            }
        }
        withStyle(SpanStyle(color = prefixColor, fontWeight = prefixWeight, fontSize = prefixSize)) {
            append("：")
        }
    }
}

internal fun AnnotatedString.Builder.appendRichTrailing(runtime: RichTextRenderContext, inline: MutableMap<String, InlineTextContent>) {
    val fontSize = runtime.style.fontSize
    val trailingContent = runtime.decorations.trailingContent
    val trailingWidth = runtime.style.trailingWidth
    if (trailingContent != null) {
        appendInlineContent("trailing", "赞")
        inline["trailing"] = InlineTextContent(
            Placeholder(
                width = trailingWidth,
                height = fontSize * 1.35f,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            ),
        ) { _ ->
            trailingContent()
        }
    }
}
