package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackPhase
import com.aritxonly.bdfun.feature.video.application.PlaybackTrackingStateHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BindPlayerProgressTracking(
    playback: PlaybackController,
    bvid: String,
    ui: VideoPlayerUiStateHolder,
    trackingController: PlaybackTrackingStateHolder, page: PlayerPageHost,
    progress: com.aritxonly.bdfun.core.domain.PlaybackProgressRepository, resumePlayback: () -> Boolean,
) {
    val state = playback.state.collectAsState()
    val playing by remember(playback) { derivedStateOf { state.value.playing } }
    val ready by remember(playback) { derivedStateOf { state.value.ready } }
    val buffering by remember(playback) {
        derivedStateOf { state.value.phase == PlaybackPhase.Buffering || state.value.phase == PlaybackPhase.Idle }
    }
    var lastProgressSaveAt by ui.lastProgressSaveAt
    val mediaSession by ui.mediaSession
    val speed by ui.speed
    val historyScope = rememberCoroutineScope()
    LaunchedEffect(playing, ready) {
        while (ready) {
            val tick = playback.refresh()
            val positionMs = tick.positionMs
            val bufferedMs = tick.bufferedMs
            val durationMs = tick.durationMs
            val playing = tick.playing
            val ended = tick.ended
            page.danmakuProgressMs = positionMs
            // 常显进度条：每秒采一次，画的时候才读，播放器本体不跟着重组。
            ui.progressPositionMs.longValue = positionMs
            ui.progressDurationMs.longValue = durationMs
            // 上报官方历史记录：首次 3 秒后，之后每 15 秒一次；播放结束时补一次
            val now = System.currentTimeMillis()
            if (resumePlayback() && now - lastProgressSaveAt >= 10_000L) {
                lastProgressSaveAt = now
                ui.preparedMediaKey.value?.split(":")?.let { key ->
                    progress.save(key[0], key.getOrNull(1)?.toLongOrNull() ?: 0L, positionMs, durationMs)
                }
            }
            // 独立协程保持原 300ms 轮询节奏，节流与 busy 由纯状态容器处理。
            historyScope.launch { trackingController.reportIfDue(page.aid, page.cid, tick, now) }
            mediaSession?.onPlayerState(
                playing = playing,
                buffering = buffering,
                positionMs = positionMs,
                durationMs = durationMs,
                speed = speed,
            )
            // 进度与通知每秒一次即可：播放/暂停/缓冲这些离散状态由播放器回调即时发布。
            delay(1_000L)
        }
    }

}
