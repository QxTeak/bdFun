package com.aritxonly.bdfun.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.aritxonly.bdfun.AppGraph
import kotlinx.coroutines.delay

internal class FeedPullSession(
    private val state: FeedState,
    private val ui: FeedPullStateHolder,
    val thresholdPx: Float,
    private val maxPullPx: Float,
    private val loggedIn: Boolean,
) {
    suspend fun animatePullTo(
        to: Float,
        durationMs: Float = 220f,
        easing: androidx.compose.animation.core.Easing =
            androidx.compose.animation.core.FastOutSlowInEasing,
    ) {
        val from = ui.pull
        if (from == to) return
        if (durationMs <= 0f) {
            ui.pull = to
            return
        }
        val start = System.nanoTime()
        while (true) {
            val p = ((System.nanoTime() - start) / 1_000_000f / durationMs)
                .coerceIn(0f, 1f)
            ui.pull = from + (to - from) * easing.transform(p)
            if (p >= 1f) break
            delay(8)
        }
        ui.pull = to
    }

    fun connection(): NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y < 0f && ui.pull > 0f) {
                    val next = (ui.pull + available.y).coerceAtLeast(0f)
                    val consumed = next - ui.pull
                    ui.pull = next
                    return Offset(0f, consumed)
                }
                if (available.y > 0f && !state.listState.canScrollBackward) {
                    val next = (ui.pull + available.y * 0.6f).coerceAtMost(maxPullPx)
                    val consumed = next - ui.pull
                    ui.pull = next
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity,
            ): androidx.compose.ui.unit.Velocity {
                val shouldRefresh = ui.pull >= thresholdPx && !state.refreshing
                if (shouldRefresh) {
                    animatePullTo(thresholdPx, 140f)
                    state.pullFraction = 1f
                    state.refresh(AppGraph.session.value != null || loggedIn)
                    // 回落交给 LaunchedEffect(state.refreshing)，这里不要再动，
                    // 否则会和首页那套「160ms 后 260ms 缓动」打架，看起来就是“唰”一下弹回。
                } else {
                    animatePullTo(0f, 220f)
                    state.pullFraction = 0f
                }
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
}
