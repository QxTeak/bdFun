package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.DanmakuItem

/** 沿用播放器原规则与去重顺序，计数取likeCount而非合并重复数。 */
fun selectHotDanmaku(danmaku: List<DanmakuItem>, now: Long): List<Pair<String, Long>> =
    selectHotDanmakuItems(danmaku, now).map { it.text to it.likeCount }

fun selectHotDanmakuItems(danmaku: List<DanmakuItem>, now: Long, maxItems: Int = 2): List<DanmakuItem> {
    val from = (now - 15_000L).coerceAtLeast(0L)
    return rankedHotDanmaku(danmaku.asSequence().filter { it.timeMs in from..now }, maxItems)
}

data class HotDanmakuWindow(
    val visible: List<DanmakuItem> = emptyList(),
    val upcoming: List<DanmakuItem> = emptyList(),
)

/** 一次索引候选；播放时只扫描当前和预留所覆盖的时间段。 */
class HotDanmakuTimeline(items: List<DanmakuItem>) {
    private val ordered = items.filter(::isHotDanmakuCandidate).sortedBy { it.timeMs }

    fun windowAt(positionMs: Long, leadMs: Long): HotDanmakuWindow {
        val now = positionMs.coerceAtLeast(0L)
        val lead = leadMs.coerceIn(0L, Long.MAX_VALUE - now)
        val currentStart = lowerBound((now - 15_000L).coerceAtLeast(0L))
        val upcomingStart = upperBound(now)
        val upcomingEnd = upperBound(now + lead)
        return HotDanmakuWindow(
            rankedHotDanmaku(ordered.subList(currentStart, upcomingStart).asSequence(), Int.MAX_VALUE),
            ordered.subList(upcomingStart, upcomingEnd),
        )
    }

    private fun lowerBound(timeMs: Long): Int = bound(timeMs, inclusive = false)
    private fun upperBound(timeMs: Long): Int = bound(timeMs, inclusive = true)

    private fun bound(timeMs: Long, inclusive: Boolean): Int {
        var low = 0
        var high = ordered.size
        while (low < high) {
            val mid = (low + high) ushr 1
            val candidate = ordered[mid].timeMs
            if (candidate < timeMs || (inclusive && candidate == timeMs)) low = mid + 1 else high = mid
        }
        return low
    }
}

private fun isHotDanmakuCandidate(item: DanmakuItem): Boolean =
    item.id > 0L && item.text.isNotBlank() && item.likeCount >= 10L

private fun rankedHotDanmaku(items: Sequence<DanmakuItem>, maxItems: Int) = items
    .filter(::isHotDanmakuCandidate)
    .sortedWith(compareByDescending<DanmakuItem> { it.likeCount }.thenBy { it.id })
    .distinctBy { it.id }
    .distinctBy { it.text }
    .take(maxItems.coerceAtLeast(0))
    .toList()

/** 渲染时间换算到媒体时间，并给播放器状态采样留一段余量。 */
fun hotDanmakuReservationLeadMs(scrollMs: Long, pinnedMs: Long, playbackSpeed: Float): Long =
    (maxOf(0L, scrollMs, pinnedMs) * playbackSpeed.coerceIn(0.1f, 5f)).toLong() + 1_000L

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
