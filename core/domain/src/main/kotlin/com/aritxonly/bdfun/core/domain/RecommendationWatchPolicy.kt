package com.aritxonly.bdfun.core.domain

import kotlin.math.*

/** 区间合并防止循环刷分；seek 不通过连续性校验，不把跳过的内容算成观看。 */
class RecommendationWatchPolicy {
    private var previousPosition: Long? = null
    private var previousTime: Long? = null
    private val ranges = mutableListOf<LongRange>()
    var wallTimeMs = 0L
        private set
    val contentTimeMs: Long get() = ranges.sumOf { it.last - it.first }

    fun sample(position: Long, time: Long, playing: Boolean, speed: Float) {
        val oldPosition = previousPosition
        val oldTime = previousTime
        previousPosition = if (playing) position else null
        previousTime = if (playing) time else null
        if (!playing || oldPosition == null || oldTime == null || !speed.isFinite()) return
        val elapsed = time - oldTime
        val delta = position - oldPosition
        if (elapsed !in 1..2500 || delta <= 0 || abs(delta - elapsed * speed) > max(350.0, elapsed * speed * 0.25)) return
        wallTimeMs = (wallTimeMs + elapsed).coerceAtMost(30 * 60_000L)
        var start: Long = oldPosition
        var end = position
        val overlap = ranges.filter { it.first <= end && it.last >= start }
        overlap.forEach { start = min(start, it.first); end = max(end, it.last) }
        ranges.removeAll(overlap.toSet())
        if (ranges.size < 512) ranges += start..end
    }

    fun satisfaction(duration: Long): Double {
        if (duration <= 0 || wallTimeMs < 5000) return 0.0
        val ratio = (contentTimeMs.toDouble() / duration).coerceIn(0.0, 1.0)
        val longWatch = (min(contentTimeMs, wallTimeMs).toDouble() / 300_000).coerceIn(0.0, 1.0)
        return max(ratio, longWatch).coerceIn(0.0, 1.0)
    }
}
