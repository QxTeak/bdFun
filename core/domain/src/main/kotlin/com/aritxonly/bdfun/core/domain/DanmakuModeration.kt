package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.DanmakuItem

enum class DanmakuRuleKind(val code: Int) {
    Keyword(0), Pattern(1), Sender(2),
}

data class DanmakuCloudRule(val id: Long, val kind: DanmakuRuleKind, val value: String)

class DanmakuRuleMatcher(rules: List<DanmakuCloudRule>) {
    private val active = rules.filter { it.value.isNotBlank() }
    private val keywords = active.filter { it.kind == DanmakuRuleKind.Keyword }.map { it.value }
    private val senders = active.filter { it.kind == DanmakuRuleKind.Sender }.map { it.value.lowercase() }.toSet()
    private val patterns = active.filter { it.kind == DanmakuRuleKind.Pattern }
        .mapNotNull { runCatching { Regex(it.value) }.getOrNull() }

    fun blocks(item: DanmakuItem): Boolean = item.userHash.lowercase() in senders ||
        keywords.any { item.text.contains(it) } || patterns.any { it.containsMatchIn(item.text) }
}

private val danmakuTimestamp = Regex("""(?<![\d:])(?:(\d{1,3}):)?(\d{1,3}):([0-5]\d)(?![\d:])""")

fun danmakuTimestampMs(text: String, durationMs: Long): Long? {
    val match = danmakuTimestamp.find(text) ?: return null
    val hours = match.groupValues[1].toLongOrNull() ?: 0
    val minutes = match.groupValues[2].toLong()
    if (hours > 0 && minutes >= 60) return null
    val position = (hours * 3600 + minutes * 60 + match.groupValues[3].toLong()) * 1000
    return position.takeIf { it in 1..durationMs }
}

data class DanmakuCloudConfig(
    val enabled: Boolean, val allowScroll: Boolean, val allowFixed: Boolean,
    val allowColor: Boolean, val allowSpecial: Boolean, val opacity: Float,
    val area: Float, val speed: Float, val fontScale: Float,
) {
    companion object {
        fun from(settings: DanmakuSettings) = DanmakuCloudConfig(
            settings.danmakuEnabled && settings.danmakuOn, !settings.danmakuBlockScroll,
            !settings.danmakuBlockFixed, !settings.danmakuBlockColor,
            !settings.danmakuBlockAdvanced, settings.danmakuOpacity,
            settings.danmakuArea, settings.danmakuSpeedPref, settings.danmakuScaleInline,
        )
    }
}
