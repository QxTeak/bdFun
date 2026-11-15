package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberPlayerSurfaceSlots(): PlayerSurfaceSlots = remember {
    PlayerSurfaceSlots { items, position, playing, speed, modifier, fullscreen, reserveTopForCounter ->
        DanmakuLayer(items = items, positionMs = position, playing = playing,
            playbackSpeed = speed, modifier = modifier, fullscreen = fullscreen,
            reserveTopForCounter = reserveTopForCounter)
    }
}
