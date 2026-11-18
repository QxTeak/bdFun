package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.DanmakuItem

/** 沿用播放器原规则与去重顺序，计数取likeCount而非合并重复数。 */
fun selectHotDanmaku(danmaku: List<DanmakuItem>, now: Long): List<Pair<String, Long>> =
    selectHotDanmakuItems(danmaku, now).map { it.text to it.likeCount }

fun selectHotDanmakuItems(danmaku: List<DanmakuItem>, now: Long): List<DanmakuItem> {
    val from = (now - 15_000L).coerceAtLeast(0L)
    return danmaku.asSequence()
        .filter { it.id > 0L && it.text.isNotBlank() && it.likeCount >= 10L }
        .filter { it.timeMs in from..now }
        .sortedWith(compareByDescending<DanmakuItem> { it.likeCount }.thenBy { it.id })
        .distinctBy { it.id }
        .distinctBy { it.text }
        .take(2)
        .toList()
}
