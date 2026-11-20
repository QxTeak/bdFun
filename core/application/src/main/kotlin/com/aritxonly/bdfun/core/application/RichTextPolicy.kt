package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.application.videoTextUrls
import com.aritxonly.bdfun.core.application.videoTextTokens
import com.aritxonly.bdfun.core.application.trimVideoTextToken

internal val EmoteToken = Regex("\\[([^\\]]+)]")

data class RichTextHit(
    val start: Int, val end: Int, val emoteToken: String? = null, val emoteUrl: String? = null,
    val mentionName: String? = null, val mentionMid: Long = 0L, val linkUrl: String? = null, val video: Boolean = false,
)
data class RichTextAnalysis(
    val hits: List<RichTextHit>, val hasEmote: Boolean, val hasMention: Boolean, val hasUrl: Boolean,
    val mentionList: List<Map.Entry<String, Long>>,
) {
    fun plainText(trailing: Boolean) = !hasEmote && !hasMention && !hasUrl && !trailing
}

/** 优先级、去重与首次 indexOf 均保留原逻辑；这里只决定区间，不依赖 Compose。 */
fun analyzeRichText(text: String, emotes: Map<String, String>, mentions: Map<String, Long>, mentionClick: Boolean): RichTextAnalysis {
    // mentions 只在开启提及点击时才需要排序结果，避免每条评论都白排一遍。
    val mentionList = if (mentionClick) mentions.entries.sortedByDescending { it.key.length } else emptyList()
    val mentionRegex = if (mentionList.isEmpty()) null
        else Regex("@(" + mentionList.joinToString("|") { Regex.escape(it.key) } + ")")
    val hits = collectRichLinks(text).toMutableList()
    addRichEmotes(hits, text, emotes)
    if (mentionRegex != null) addRichMentions(hits, text, mentionRegex, mentions)
    return RichTextAnalysis(hits.sortedBy { it.start }, emotes.isNotEmpty() && EmoteToken.containsMatchIn(text),
        mentionClick && mentionRegex?.containsMatchIn(text) == true,
        videoTextUrls.containsMatchIn(text) || videoTextTokens(text).isNotEmpty(), mentionList)
}

fun collectRichLinks(text: String): List<RichTextHit> {
    val hits = mutableListOf<RichTextHit>()
    for (match in videoTextUrls.findAll(text)) {
        val url = trimVideoTextToken(match.value)
        if (url.length <= "https://".length) continue
        hits += RichTextHit(match.range.first, match.range.first + url.length, linkUrl = url)
    }
    for (token in videoTextTokens(text)) {
        val start = text.indexOf(token)
        if (start < 0) continue
        val end = start + token.length
        if (hits.any { it.start < end && start < it.end }) continue
        hits += RichTextHit(start, end, linkUrl = token, video = true)
    }
    return hits
}

/**
 * 表情正则是按「当前账号表情快照」整体编译的：一屏评论共用同一份 map，
 * 每条评论都重新排序 + 编译一次会白白多出几百毫秒。这里按 map 实例缓存一份，
 * 快照替换（登录/刷新表情）时自然失效。
 */
private class CompiledEmotes(val source: Map<String, String>, val pattern: Regex?)
private var compiledEmotes: CompiledEmotes? = null

@Synchronized
private fun emotePattern(emotes: Map<String, String>): Regex? {
    compiledEmotes?.let { if (it.source === emotes) return it.pattern }
    // 评论自带的字典足以显示未下载的表情；完整名称可能包含内层括号。
    val tokens = emotes.keys.map { it.removeSurrounding("[", "]") }
        .filter { it.isNotEmpty() }.distinct().sortedByDescending { it.length }
    val pattern = if (tokens.isEmpty()) null
        else Regex(tokens.joinToString("|") { Regex.escape("[$it]") })
    compiledEmotes = CompiledEmotes(emotes, pattern)
    return pattern
}

private fun addRichEmotes(hits: MutableList<RichTextHit>, text: String, emotes: Map<String, String>) {
    val pattern = emotePattern(emotes) ?: return
    for (match in pattern.findAll(text)) {
        val token = match.value.removeSurrounding("[", "]")
        val url = emotes[token] ?: emotes[match.value] ?: continue
        val start = match.range.first; val end = match.range.last + 1
        if (hits.any { it.start < end && start < it.end }) continue
        hits += RichTextHit(start, end, emoteToken = token, emoteUrl = url)
    }
}

private fun addRichMentions(hits: MutableList<RichTextHit>, text: String, regex: Regex, mentions: Map<String, Long>) {
    for (match in regex.findAll(text)) {
        val name = match.groupValues[1]
        val mid = mentions[name] ?: continue
        val start = match.range.first; val end = match.range.last + 1
        if (hits.any { it.start < end && start < it.end }) continue
        hits += RichTextHit(start, end, mentionName = name, mentionMid = mid)
    }
}
