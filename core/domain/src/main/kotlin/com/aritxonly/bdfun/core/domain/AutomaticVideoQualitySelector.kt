package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.Quality

/** 平台读取失败或没有活动网络时返回0，单位沿用 Android 的 Kbps。 */
fun interface NetworkBandwidthSource { fun downstreamKbps(): Int }

/** 当前是不是蜂窝网络：移动数据下自动档要封顶，避免默认落到 4K/8K。 */
fun interface CellularNetworkSource { fun isCellular(): Boolean }

/** 移动网络自动选档的天花板：1080P60，不允许自动选到 4K 及以上。 */
const val CELLULAR_QUALITY_CAP = 116

/** 半屏（内联）播放的最高清晰度：720P；全屏后再恢复用户选择/自动档位。 */
const val INLINE_QUALITY_CAP = 64

/**
 * 把半屏播放的清晰度压到 720P 以内。
 *
 * 番剧/影视这类高码率源在半屏小窗里没有意义，先按 720P 起播；进入全屏时
 * 调用方不再调用本函数，自然回到原档位。
 */
fun capInlineQuality(quality: Quality?, available: List<Quality>): Quality? {
    if (quality == null || quality.id <= INLINE_QUALITY_CAP) return quality
    return available.filter { it.id <= INLINE_QUALITY_CAP }.maxByOrNull { it.id } ?: quality
}

/** 自动选档沿用常规 SDR 阶梯；没有阶梯值时候仍按旧规则选最小 id。 */
class AutomaticVideoQualitySelector(
    private val bandwidth: NetworkBandwidthSource,
    private val cellular: CellularNetworkSource = CellularNetworkSource { false },
) {
    fun select(qualities: List<Quality>): Quality? {
        if (qualities.isEmpty()) return null
        val mbps = bandwidth.downstreamKbps() / 1024.0
        val requested = when {
            mbps >= 40.0 -> 127
            mbps >= 24.0 -> 120
            mbps >= 10.0 -> 112
            mbps >= 6.0 -> 80
            mbps >= 3.0 -> 64
            mbps >= 1.5 -> 32
            mbps > 0.0 -> 16
            else -> 64
        }
        // 蜂窝网络只在自动档封顶；用户手动选 4K 不受影响。
        val target = if (cellular.isCellular()) minOf(requested, CELLULAR_QUALITY_CAP) else requested
        return ladder.firstOrNull { id -> id <= target && qualities.any { it.id == id } }
            ?.let { id -> qualities.first { it.id == id } }
            ?: qualities.minByOrNull { it.id }
    }
    private val ladder = listOf(127, 120, 116, 112, 80, 74, 64, 32, 16)
}
