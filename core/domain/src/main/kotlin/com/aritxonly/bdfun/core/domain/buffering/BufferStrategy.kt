package com.aritxonly.bdfun.core.domain.buffering

const val DEFAULT_ADAPTIVE_BUFFERING = false

data class BufferContext(val watchedMs: Long, val rebuffered: Boolean = false)
data class BufferBudget(val lowWaterMs: Long, val targetMs: Long)

fun interface CacheStrategy {
    fun budget(context: BufferContext): BufferBudget
}

class FixedCacheStrategy : CacheStrategy {
    override fun budget(context: BufferContext): BufferBudget {
        val target = when {
            context.watchedMs < 8_000 -> 8_000L
            context.watchedMs < 30_000 -> 20_000L
            else -> 35_000L
        }
        return BufferBudget(target * 2 / 5, target)
    }
}

class AdaptiveCacheStrategy(private val history: () -> WatchHistory) : CacheStrategy {
    override fun budget(context: BufferContext): BufferBudget {
        val baseline = FixedCacheStrategy().budget(context)
        val model = history()
        val confidence = (model.samples / 30.0).coerceIn(0.0, 1.0)
        val survival = model.survival(context.watchedMs, baseline.targetMs)
        // 冷启动保持规则基线；成熟模型仍只在有限范围内改变预取预算。
        val scale = 1.0 + confidence * (survival - 0.65)
        val target = (baseline.targetMs * scale).toLong().coerceIn(6_000, 45_000)
        return BufferBudget(target * 2 / 5, target)
    }
}

data class BufferInput(
    val bufferedMediaMs: Long,
    val speed: Float,
    val allocatedBytes: Int,
    val playWhenReady: Boolean = true,
)

data class BufferDecision(
    val shouldLoad: Boolean,
    val targetMediaMs: Long,
    val lowWaterMediaMs: Long,
    val additionalMediaMs: Long,
)

/** 单个播放会话持有；输出媒体时间预算，HTTP 分块与解复用继续由播放器负责。 */
class VideoBufferProvider(private val strategy: CacheStrategy) {
    private var loading = false

    fun decide(context: BufferContext, input: BufferInput): BufferDecision {
        val speed = input.speed.takeIf { it.isFinite() && it > 0 } ?: 1f
        val budget = strategy.budget(context)
        val targetWall = if (context.rebuffered) maxOf(25_000L, budget.targetMs) else budget.targetMs
        val lowWall = if (context.rebuffered) maxOf(10_000L, budget.lowWaterMs) else budget.lowWaterMs
        val target = (targetWall * speed).toLong().coerceIn(1_000, 120_000)
        val low = (lowWall * speed).toLong().coerceIn(500, target)
        val buffered = input.bufferedMediaMs.coerceAtLeast(0)
        loading = when {
            input.allocatedBytes >= MAX_BUFFER_BYTES -> false
            !input.playWhenReady && buffered >= (1_000 * speed).toLong() -> false
            buffered < low -> true
            buffered >= target -> false
            else -> loading
        }
        return BufferDecision(loading, target, low, (target - buffered).coerceAtLeast(0))
    }

    companion object { const val MAX_BUFFER_BYTES = 64 * 1024 * 1024 }
}
