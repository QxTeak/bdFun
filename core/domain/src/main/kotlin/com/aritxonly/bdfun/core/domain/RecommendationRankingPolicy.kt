package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.feed.RecommendationMemory
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlin.math.*

/** v1 保留来源批次的先验，只在三位以内调整，不借热度猜测内容质量。 */
object RecommendationRankingPolicy {
    const val VERSION = 1
    const val MAX_SHIFT = 3
    const val DAY = 86_400_000L

    fun rank(
        candidates: List<VideoItem>, memory: List<RecommendationMemory>,
        session: List<RecommendationMemory>, previous: List<VideoItem>, now: Long,
    ): List<VideoItem> {
        val items = candidates.distinctBy { it.bvid }
        val records = memory.associateBy { it.bvid }
        val scores = items.mapIndexed { index, video ->
            val record = records[video.bvid]
            val fatigue = if (record != null && record.exposedAt > 0 && now - record.exposedAt in 0..DAY) 0.15 else 0.0
            val watched = if (record != null && now - record.watchedAt in 0..7 * DAY) record.satisfaction * 0.2 else 0.0
            1.0 / log2(index + 2.0) + (interest(video, memory, now, 14 * DAY) * 0.12 +
                interest(video, session, now, 30 * 60_000L) * 0.18).coerceIn(-0.3, 0.3) - fatigue - watched
        }
        val remaining = items.indices.toMutableList()
        val selected = mutableListOf<VideoItem>()
        while (remaining.isNotEmpty()) {
            val position = selected.size
            // 最迟位置已到的条目必须出队，保证惩罚和多样性也不能突破位移边界。
            val overdue = remaining.firstOrNull { it + MAX_SHIFT <= position }
            val chosen = overdue ?: remaining.filter { it <= position + MAX_SHIFT }.maxBy { index ->
                scores[index] - diversity(items[index], (previous.takeLast(6) + selected).takeLast(6))
            }
            selected += items[chosen]
            remaining.remove(chosen)
        }
        return selected
    }

    private fun interest(video: VideoItem, records: List<RecommendationMemory>, now: Long, halfLife: Long): Double {
        val evidence = records.filter { it.satisfaction > 0 && (
            (video.ownerMid > 0 && it.owner == video.ownerMid) ||
                (video.categoryName.isNotBlank() && it.category == video.categoryName)) }
        val weights = evidence.map { exp(-ln(2.0) * (now - it.watchedAt).coerceAtLeast(0) / halfLife) }
        val count = weights.sum()
        if (count == 0.0) return 0.0
        return evidence.zip(weights).sumOf { (record, weight) -> record.satisfaction * weight } / (count + 5.0)
    }

    private fun diversity(video: VideoItem, previous: List<VideoItem>): Double {
        if (previous.isEmpty()) return 0.0
        val owner = video.ownerMid.takeIf { it > 0 } ?: return 0.0
        val adjacent = if (previous.last().ownerMid == owner) 0.15 else 0.0
        val repeated = if (previous.count { it.ownerMid == owner } >= 2) 0.08 else 0.0
        val category = if (video.categoryName.isNotBlank() && previous.takeLast(2).all {
            it.categoryName == video.categoryName }) 0.03 else 0.0
        return adjacent + repeated + category
    }
}
