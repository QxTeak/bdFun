package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import com.aritxonly.bdfun.core.domain.selectHotDanmakuItems
import kotlin.math.abs

@Composable
fun BindPlayerHotDanmaku(
    playback: PlaybackController,
    danmaku: List<DanmakuItem>,
    danmakuVisible: Boolean,
    ui: VideoPlayerUiStateHolder,
) {
    val state = playback.state.collectAsState()
    val positionMs by remember(playback) { derivedStateOf { state.value.positionMs } }
    var hotDanmaku by ui.hotDanmaku
    // 弹幕计数：完全照 BiliPai：用分段弹幕接口里的点赞数 like，
    // 只取最近 15 秒、点赞 ≥ 10 的弹幕，按点赞降序、id / 文字去重后显示前两条。
    LaunchedEffect(danmaku, danmakuVisible) {
        if (!danmakuVisible) {
            hotDanmaku = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            val now = positionMs
            hotDanmaku = selectHotDanmakuItems(danmaku, now)
            kotlinx.coroutines.delay(1_000L)
        }
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
