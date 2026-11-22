package com.aritxonly.bdfun.core.domain

/** B 站弹幕分段固定 6 分钟一段，下标从 1 开始。 */
const val DANMAKU_SEGMENT_MS = 360_000L

/** 播放位置所在的分段下标（1 起）。 */
fun danmakuSegmentIndex(positionMs: Long): Int =
    (positionMs.coerceAtLeast(0L) / DANMAKU_SEGMENT_MS).toInt() + 1

/** 视频总分段数；没有时长时至少返回 1，避免上层除零/空列表。 */
fun danmakuSegmentCount(durationSec: Int): Int =
    if (durationSec <= 0) 1 else ((durationSec + 359) / 360)

/**
 * 当前播放位置需要保证已加载的分段：当前段前后各一段。
 *
 * 参考 biliPai：只维持一个 ±1 的滑动窗口并复用缓存，不再一次性拉完整支视频。
 */
fun danmakuSegmentWindow(positionMs: Long, totalSegments: Int = Int.MAX_VALUE): List<Int> {
    val anchor = danmakuSegmentIndex(positionMs).coerceIn(1, totalSegments.coerceAtLeast(1))
    return ((anchor - 1)..(anchor + 1)).filter { it in 1..totalSegments }
}
