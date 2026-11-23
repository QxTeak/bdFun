package com.aritxonly.bdfun.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform
import kotlinx.coroutines.delay

@Composable
fun BindPlayerControlVisibility(
    playback: VideoPlaybackPlatform,
    fullscreen: Boolean,
    playing: Boolean,
    ui: VideoPlayerUiStateHolder, page: PlayerPageHost,
) {
    val context = LocalContext.current
    var controlsVisible by ui.controlsVisible
    var speedMenu by ui.speedMenu
    var fsPanel by ui.fsPanel
    var fsInputOpen by ui.fsInputOpen
    var fsAudioOnly by ui.fsAudioOnly
    var lastFullscreenForControls by ui.lastFullscreenForControls
    DisposableEffect(playing) {
        val act = context as? Activity
        if (playing) {
            act?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(controlsVisible) { page.controlsVisible = controlsVisible }
    DisposableEffect(Unit) {
        onDispose { page.controlsVisible = false }
    }

    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            speedMenu = false
            fsPanel = FullscreenPanel.None
            fsInputOpen = false
        }
    }

    LaunchedEffect(fullscreen) {
        if (fullscreen == lastFullscreenForControls) return@LaunchedEffect
        lastFullscreenForControls = fullscreen
        // 进入 / 退出全屏（包括旋转触发）都不自动弹出控件，全部先收起。
        // 例外：听视频模式已经关掉视频轨，全屏界面本身就是唯一的「画面」，
        // 收起控件等于整屏黑掉，所以进入全屏时保持可见（它本来也不参与自动隐藏）。
        val keepAudioChrome = fullscreen && fsAudioOnly
        controlsVisible = keepAudioChrome
        page.controlsVisible = keepAudioChrome
        if (!fullscreen) {
            fsPanel = FullscreenPanel.None
            fsInputOpen = false
            // 听视频不在退出全屏时关闭：旋转是「横屏全屏 → 竖屏半屏」的同一次播放，
            // 把模式清掉会让用户刚开的听视频自己退出。真正退出只用听视频界面上的按钮。
        }
    }

    LaunchedEffect(fsAudioOnly) {
        if (fsAudioOnly) {
            controlsVisible = true
            page.controlsVisible = true
            // 听视频界面没有锁按钮，若带着旧的锁定状态进来就再也解不开了。
            ui.fsLocked.value = false
        }
        // 听视频：真正关掉视频轨，只保留音频解码，不是拿一张图盖住画面
        playback.setVideoEnabled(!fsAudioOnly)
    }

    BindPlayerControlsAutoHide(ui, page)
}

@Composable
private fun BindPlayerControlsAutoHide(ui: VideoPlayerUiStateHolder, page: PlayerPageHost) {
    var controlsVisible by ui.controlsVisible
    val interactionTick by ui.interactionTick
    val boosting by ui.boosting
    val fsPanel by ui.fsPanel
    val fsInputOpen by ui.fsInputOpen
    val fsAudioOnly by ui.fsAudioOnly
    val fsScrubbing by ui.fsScrubbing
    LaunchedEffect(
        controlsVisible,
        interactionTick,
        boosting,
        fsPanel,
        fsInputOpen,
        fsAudioOnly,
        fsScrubbing,
    ) {
        if (controlsVisible && !boosting &&
            fsPanel == FullscreenPanel.None && !fsInputOpen && !fsAudioOnly &&
            !fsScrubbing
        ) {
            delay(HIDE_DELAY_MS)
            controlsVisible = false
            page.controlsVisible = false
        }
    }

}

/** 原控件4秒自动隐藏；搬迁不改变交互时序。 */
private const val HIDE_DELAY_MS = 4000L
