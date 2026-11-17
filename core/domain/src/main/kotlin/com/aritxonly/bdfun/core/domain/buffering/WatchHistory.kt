package com.aritxonly.bdfun.core.domain.buffering

import kotlin.math.exp

/** 固定六档聚合，不保存视频标识或原始观看轨迹。 */
data class WatchHistory(
    val exposureSeconds: List<Double> = List(6) { 0.0 },
    val exits: List<Double> = List(6) { 0.0 },
    val samples: Double = 0.0,
) {
    fun observe(watchedMs: Long, abandoned: Boolean): WatchHistory {
        if (watchedMs <= 0) return this
        val seconds = watchedMs / 1000.0
        val exposure = exposureSeconds.mapIndexed { i, old ->
            old * DECAY + (seconds - EDGES[i]).coerceIn(0.0, EDGES[i + 1] - EDGES[i])
        }
        val terminal = EDGES.indexOfLast { seconds >= it }.coerceIn(0, 5)
        val events = exits.mapIndexed { i, old ->
            old * DECAY + if (abandoned && i == terminal) 1.0 else 0.0
        }
        return WatchHistory(exposure, events, samples * DECAY + 1)
    }

    fun survival(watchedMs: Long, horizonMs: Long): Double {
        val start = watchedMs.coerceAtLeast(0) / 1000.0
        val end = start + horizonMs.coerceAtLeast(0) / 1000.0
        var hazard = 0.0
        for (i in 0..5) {
            val overlap = (minOf(end, EDGES[i + 1]) - maxOf(start, EDGES[i])).coerceAtLeast(0.0)
            // 两个先验事件和六十秒曝光抑制少量样本造成的极端预测。
            hazard += overlap * (exits[i] + 2.0) / (exposureSeconds[i] + 60.0)
        }
        return exp(-hazard).coerceIn(0.0, 1.0)
    }

    companion object {
        private const val DECAY = 0.98
        private val EDGES = listOf(0.0, 3.0, 8.0, 15.0, 30.0, 60.0, Double.POSITIVE_INFINITY)
    }
}

interface BufferHistoryStore {
    fun read(account: String): WatchHistory
    fun observe(account: String, watchedMs: Long, abandoned: Boolean)
}
