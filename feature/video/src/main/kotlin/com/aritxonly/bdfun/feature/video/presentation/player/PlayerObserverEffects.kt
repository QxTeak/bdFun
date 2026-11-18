package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackPhase
import com.aritxonly.bdfun.core.application.PlaybackFailure
import com.aritxonly.bdfun.core.domain.PlaybackRecoveryAction
import com.aritxonly.bdfun.core.domain.selectPlaybackRecovery

@Composable
fun BindPlayerObserver(
    playback: PlaybackController,
    media: VideoMediaInput,
    actions: VideoPlayerActions,
    ui: VideoPlayerUiStateHolder, page: PlayerPageHost,
    progress: com.aritxonly.bdfun.core.domain.PlaybackProgressRepository,
    exit: PlayerExitHooks, diagnostics: PlayerDiagnostics,
) {
    val bvid = media.bvid
    val info = media.info
    val quality = media.quality
    val latestBvid by rememberUpdatedState(bvid)
    val onPlayStateChanged = actions.transport.onPlayStateChanged
    var controlsVisible by ui.controlsVisible
    val latestInfo by rememberUpdatedState(info)
    val latestQuality by rememberUpdatedState(quality)
    DisposableEffect(Unit) {
        val listener = object : PlaybackController.Listener {
            override fun onRenderedFirstFrame() {
                ui.renderedMediaKey.value = ui.preparedMediaKey.value
                diagnostics.log(
                    "PLAYER",
                    "first-frame bvid=$bvid q=${latestQuality?.id}",
                )
            }

            override fun onPlayingChanged(isPlaying: Boolean) {
                onPlayStateChanged(isPlaying)
            }

            override fun onPhaseChanged(state: PlaybackPhase) {
                if (state == PlaybackPhase.Buffering || state == PlaybackPhase.Ready) {
                    diagnostics.log(
                        "PLAYER",
                        "state=${if (state == PlaybackPhase.Buffering) "buffering" else "ready"} " +
                            "bvid=$bvid q=${latestQuality?.id}",
                    )
                }
                if (state == PlaybackPhase.Ready) {
                    playback.acknowledgeEnd()
                }
                if (state == PlaybackPhase.Ended) {
                    controlsVisible = true
                    page.controlsVisible = true
                }
            }

            override fun onError(error: PlaybackFailure) {
                handlePlayerError(diagnostics.toast, error, latestQuality, latestInfo, ui, diagnostics.log)
            }
        }
        playback.addListener(listener)
        onDispose {
            if (exit.resumePlayback()) {
                runCatching {
                    val tick = playback.refresh()
                    ui.preparedMediaKey.value?.split(":")?.let { key ->
                        progress.save(key[0], key.getOrNull(1)?.toLongOrNull() ?: 0L, tick.positionMs, tick.durationMs)
                    }
                }
            }
            playback.removeListener(listener)
            val handOff = exit.showDiscAfterVideo() && !exit.stopOnVideoPageExit() &&
                exit.shouldHandOff() &&
                playback.refresh().phase != PlaybackPhase.Idle
            if (handOff) {
                // 页面正常关闭，但播放器继续活着：系统一镜到底退出动画照常播。
                exit.adopt(playback)
            } else {
                exit.cancelHandOff()
                playback.release()
            }
        }
    }

}

private fun handlePlayerError(
    toast: (String) -> Unit,
    error: PlaybackFailure,
    latestQuality: com.aritxonly.bdfun.core.model.video.Quality?,
    latestInfo: com.aritxonly.bdfun.core.model.video.PlayInfo?,
    ui: VideoPlayerUiStateHolder,
    log: (String, String) -> Unit,
) {
    var usingOriginal by ui.usingOriginal
    var usingFallback by ui.usingFallback
    log(
        "PLAYER",
        "error code=${error.code} q=${latestQuality?.id} " +
            "usingOriginal=$usingOriginal usingFallback=$usingFallback " +
            "message=${error.message?.take(300)}",
    )
    if (com.aritxonly.bdfun.core.domain.isTransientPlaybackNetworkFailure(error.code)) {
        toast("网络连接中断，请检查网络后重试")
        return
    }
    when (selectPlaybackRecovery(latestQuality, latestInfo, usingOriginal, usingFallback)) {
        PlaybackRecoveryAction.RetryOriginal -> usingOriginal = true
        PlaybackRecoveryAction.RetryFallback -> {
            usingFallback = true
            toast("当前清晰度播放失败，已切换到兼容画质")
        }
        PlaybackRecoveryAction.Fail -> toast("播放失败，请尝试切换清晰度")
    }
}
