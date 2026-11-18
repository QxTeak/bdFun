package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.DanmakuItem

/** 沿用播放器原规则与去重顺序，计数取likeCount而非合并重复数。 */
fun selectHotDanmaku(danmaku: List<DanmakuItem>, now: Long): List<Pair<String, Long>> =
    selectHotDanmakuItems(danmaku, now).map { it.text to it.likeCount }

fun selectHotDanmakuItems(danmaku: List<DanmakuItem>, now: Long, maxItems: Int = 2): List<DanmakuItem> {
    val from = (now - 15_000L).coerceAtLeast(0L)
    return danmaku.asSequence()
        .filter { it.id > 0L && it.text.isNotBlank() && it.likeCount >= 10L }
        .filter { it.timeMs in from..now }
        .sortedWith(compareByDescending<DanmakuItem> { it.likeCount }.thenBy { it.id })
        .distinctBy { it.id }
        .distinctBy { it.text }
        .take(maxItems.coerceAtLeast(0))
        .toList()
}

/** 只显示可完整放下的条目，按宽度从小到大选，条目之间及两端留空。 */
fun fittingHotDanmakuIndices(widths: List<Float>, availablePx: Float, gapPx: Float): List<Int> {
    var occupied = gapPx
    return widths.indices.asSequence()
        .filter { widths[it].isFinite() && widths[it] > 0f }
        .sortedBy { widths[it] }
        .takeWhile { index ->
            occupied += widths[index] + gapPx
            occupied <= availablePx
        }
        .take(3)
        .toList()
}
