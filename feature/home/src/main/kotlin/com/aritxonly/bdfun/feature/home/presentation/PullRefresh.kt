package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.delay

/**
 * 推荐 / 直播 / 番剧 / 动态共用的下拉刷新会话（"小电视"那一套手势）。
 *
 * 之前四个页面各有一份几乎相同的 HomePullSession / DiscoverPullSession / FeedPullSession，
 * 这里合并成一份：8ms 手动缓动、0.6 阻尼、阈值与上限由调用方按 dp 传入，
 * 松手过阈值就刷新、没过就回弹。调用方只负责把自己的 px/dp/fraction 写回状态。
 */
class PullRefreshSession(
    val thresholdPx: Float,
    private val maxPullPx: Float,
    private val density: Density,
    private val currentOffset: () -> Float,
    /** 把当前下拉距离写回页面：px、dp、0..1 的进度。 */
    private val onPull: (px: Float, dp: Float, fraction: Float) -> Unit,
    private val canScrollBackward: () -> Boolean,
    private val isRefreshing: () -> Boolean,
    /** 松手达到阈值：各页在这里把进度顶到 1 并开始刷新。 */
    private val onRefresh: suspend () -> Unit,
    /** 松手没达到阈值：各页把进度归零。 */
    private val onCanceled: () -> Unit = {},
) {
    private fun applyPullRaw(px: Float) {
        val next = px.coerceIn(0f, maxPullPx)
        onPull(next, with(density) { next.toDp().value }, (next / thresholdPx).coerceIn(0f, 1f))
    }

    suspend fun animatePullTo(
        target: Float,
        durationMs: Float = 220f,
        easing: Easing = FastOutSlowInEasing,
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
            val p = ((System.nanoTime() - startNanos) / 1_000_000f / durationMs).coerceIn(0f, 1f)
            applyPullRaw(from + (to - from) * easing.transform(p))
            if (p >= 1f) break
            delay(8)
        }
        applyPullRaw(to)
    }

    fun connection(): NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                // 已经拉开时，往上滑先收回下拉距离。
                if (available.y < 0f && currentOffset() > 0f) {
                    val next = (currentOffset() + available.y).coerceAtLeast(0f)
                    val consumed = next - currentOffset()
                    applyPullRaw(next)
                    return Offset(0f, consumed)
                }
                // 列表已经在顶部时，继续下拉按 0.6 阻尼拉开。
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
                    // 回落交给 PullRefreshSettleEffect，这里保持在阈值位置。
                } else {
                    animatePullTo(0f, 220f)
                    onCanceled()
                }
                return Velocity.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
        }
}

/**
 * 刷新结束后的收尾：先把进度归零，等 160ms 让小电视收掉，再 260ms 平滑回弹。
 * 四个页面共用同一条时序，不再各写一份 LaunchedEffect。
 */
@Composable
fun PullRefreshSettleEffect(
    refreshing: Boolean,
    currentOffset: () -> Float,
    onSettledStart: () -> Unit,
    animatePull: suspend (targetPx: Float, durationMs: Float) -> Unit,
) {
    LaunchedEffect(refreshing) {
        if (!refreshing && currentOffset() > 0f) {
            onSettledStart()
            delay(160)
            animatePull(0f, 260f)
        }
    }
}
