package com.aritxonly.bdfun.presentation.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity

/**
 * 直播 / 番剧页的下拉刷新：参数与缓动完全照抄推荐页的 HomePullSession
 * （8ms 手动缓动、0.6 阻尼、88dp 阈值、260dp 上限），保证手势手感一致。
 */
internal class DiscoverPullSession(
    private val density: Density,
    private val thresholdPx: Float,
    private val maxPullPx: Float,
    private val currentOffset: () -> Float,
    private val onPull: (Float) -> Unit,
    private val onPullDp: (Float) -> Unit,
    private val canScrollBackward: () -> Boolean,
    private val isRefreshing: () -> Boolean,
    private val onRefresh: () -> Unit,
) {
    private fun applyPullRaw(px: Float) {
        val next = px.coerceIn(0f, maxPullPx)
        onPull(next)
        onPullDp(with(density) { next.toDp().value })
    }

    suspend fun animatePullTo(
        target: Float,
        durationMs: Float = 220f,
        easing: androidx.compose.animation.core.Easing =
            androidx.compose.animation.core.FastOutSlowInEasing,
    ) {
        val from = currentOffset()
        val to = target.coerceIn(0f, maxPullPx)
        if (from == to) return
        if (durationMs <= 0f) {
            applyPullRaw(to)
            return
        }
        val startNanos = System.nanoTime()
        while (true) {
            val p = ((System.nanoTime() - startNanos) / 1_000_000f / durationMs)
                .coerceIn(0f, 1f)
            applyPullRaw(from + (to - from) * easing.transform(p))
            if (p >= 1f) break
            kotlinx.coroutines.delay(8)
        }
        applyPullRaw(to)
    }

    fun connection(): NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y < 0f && currentOffset() > 0f) {
                    val next = (currentOffset() + available.y).coerceAtLeast(0f)
                    val consumed = next - currentOffset()
                    applyPullRaw(next)
                    return Offset(0f, consumed)
                }
                if (available.y > 0f && !canScrollBackward()) {
                    val next = (currentOffset() + available.y * 0.6f).coerceAtMost(maxPullPx)
                    val consumed = next - currentOffset()
                    applyPullRaw(next)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val shouldRefresh = currentOffset() >= thresholdPx && !isRefreshing()
                if (shouldRefresh) {
                    animatePullTo(thresholdPx, 140f)
                    onRefresh()
                } else {
                    animatePullTo(0f, 220f)
                }
                return Velocity.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
        }
}

internal class DiscoverPullUi {
    var offset by mutableFloatStateOf(0f)
}
