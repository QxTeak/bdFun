package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.buildAnnotatedString
import com.aritxonly.bdfun.core.application.*

internal fun buildRichText(runtime: RichTextRenderContext, inline: MutableMap<String, InlineTextContent>): AnnotatedString {
    inline.clear()
    return buildAnnotatedString {
        appendRichPrefix(runtime, inline)
        appendRichBody(runtime, inline)
        appendRichTrailing(runtime, inline)
    }
}

private fun AnnotatedString.Builder.appendRichBody(runtime: RichTextRenderContext, inline: MutableMap<String, InlineTextContent>) {
    val text = runtime.content.text
    var index = 0
    var position = 0
    for (hit in runtime.analysis.hits) {
        if (hit.start < index) continue
        if (hit.start > index) append(text.substring(index, hit.start))
        if (hit.emoteUrl != null) appendRichEmote(runtime, inline, hit, "emote-${position++}")
        else if (hit.linkUrl != null) appendRichLink(runtime, inline, hit)
        else if (hit.timestampSeconds != null) appendRichTimestamp(runtime, hit)
        else appendRichMention(runtime, inline, hit)
        index = hit.end
    }
    if (index < text.length) append(text.substring(index))
}
