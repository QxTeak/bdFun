package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.domain.HotDanmakuTimeline
import com.aritxonly.bdfun.core.domain.HotDanmakuWindow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@Composable
fun BindPlayerHotDanmaku(
    playback: PlaybackController,
    danmakuVisible: Boolean,
    ui: VideoPlayerUiStateHolder,
) {
    val danmaku by ui.danmakuHit.filteredSnapshot
    val leadMs by ui.danmakuHit.hotReservationLeadMs
    val timeline = remember(danmaku) { HotDanmakuTimeline(danmaku) }
    LaunchedEffect(playback, timeline, danmakuVisible, leadMs) {
        if (!danmakuVisible) {
            ui.hotDanmaku.value = HotDanmakuWindow()
            return@LaunchedEffect
        }
        playback.state.map { it.positionMs.coerceAtLeast(0L) / 250L }
            .distinctUntilChanged()
            .collect { ui.hotDanmaku.value = timeline.windowAt(playback.state.value.positionMs, leadMs) }
    }

}

@Composable
fun BindPlayerDanmakuClock(
    playback: PlaybackController,
    playing: Boolean,
    danmakuVisible: Boolean,
    ui: VideoPlayerUiStateHolder,
) {
    var frameMs by ui.frameMs
    val boosting by ui.boosting
    val boostSpeed by ui.boostSpeed
    val speed by ui.speed
    LaunchedEffect(playing, danmakuVisible) {
        if (!playing || !danmakuVisible) return@LaunchedEffect
        var lastEmitNs = 0L
        val frameIntervalNs = 16_666_667L // 弹幕最多 60 帧，降低 120Hz 下的无效重绘
        while (true) {
            withFrameNanos { frameTimeNs ->
                if (lastEmitNs == 0L) {
                    frameMs = playback.refresh().positionMs
                    lastEmitNs = frameTimeNs
                } else if (frameTimeNs - lastEmitNs >= frameIntervalNs) {
                    val deltaMs = (frameTimeNs - lastEmitNs) / 1_000_000.0
                    // 用界面上的目标倍速，而不是等播放器内部慢慢生效的倍速，
                    // 这样长按加速/松开恢复时弹幕会立刻跟着变，不会晚半拍。
                    val rate = if (boosting) boostSpeed else speed
                    val predicted = frameMs + (deltaMs * rate).toLong()
                    val actual = playback.refresh().positionMs
                    val error = actual - predicted
                    frameMs = if (abs(error) > 2_500L) {
                        // 拖动进度条等大跳变：直接回同步。
                        actual
                    } else {
                        // 小偏差每次最多修正 4ms，避免画面一卡一卡。
                        predicted + error.coerceIn(-4L, 4L)
                    }
                    lastEmitNs = frameTimeNs
                }
            }
        }
    }

}
