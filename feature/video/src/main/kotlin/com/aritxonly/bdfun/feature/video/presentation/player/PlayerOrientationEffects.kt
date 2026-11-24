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
    fullscreen: Boolean, ui: VideoPlayerUiStateHolder, page: PlayerPageHost,
    portraitVideo: Boolean = false,
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
    LaunchedEffect(fsLocked, fullscreen, portraitVideo) {
        page.orientationLocked = fsLocked && fullscreen
        if (fsLocked && fullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else if (fullscreen) {
            activity?.requestedOrientation = if (portraitVideo) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { page.orientationLocked = false }
    }
}
