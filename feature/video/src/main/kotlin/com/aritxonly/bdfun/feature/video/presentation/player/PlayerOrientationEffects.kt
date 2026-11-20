package com.aritxonly.bdfun.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

@Composable
fun BindPlayerOrientation(
    fullscreen: Boolean, isVertical: Boolean, ui: VideoPlayerUiStateHolder, page: PlayerPageHost,
) {
    val activity = LocalContext.current as? Activity
    var fsLockHint by ui.fsLockHint
    val fsLocked by ui.fsLocked
    LaunchedEffect(fsLockHint) {
        if (fsLockHint) {
            kotlinx.coroutines.delay(3_000L)
            fsLockHint = false
        }
    }
    LaunchedEffect(fsLocked, fullscreen, isVertical) {
        // 竖屏视频全屏保持竖屏：锁方向同时也挡住 Activity 的传感器转屏监听器。
        val verticalFullscreen = fullscreen && isVertical
        page.orientationLocked = (fsLocked && fullscreen) || verticalFullscreen
        if (fsLocked && fullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else if (verticalFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else if (fullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
    DisposableEffect(Unit) {
        onDispose { page.orientationLocked = false }
    }
}
