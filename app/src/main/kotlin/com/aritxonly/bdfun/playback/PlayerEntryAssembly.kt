package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberPlayerEntryPorts(): PlayerEntryPorts {
    val context = LocalContext.current
    val page = LocalVideoPageState.current
    val settings = page.settings
    val dependencies = page.dependencies
    val engine = PlayerEnginePorts(page, dependencies.background, dependencies.progress,
        rememberPlayerExitHooks(), rememberPlayerDiagnostics(),
        PlayerEngineSettings({ settings.backgroundPlay },
            { settings.boostDefaultSpeed }, { settings.autoPlay }, { settings.resumePlayback },
            { settings.autoPlayNext = it }, { settings.loopMode = it }))
    return PlayerEntryPorts(
        createPlayback = { dependencies.createPlayback(context, runCatching { settings.decoderMode }.getOrDefault(0)) },
        tracking = { dependencies.controllers.tracking() },
        defaultSpeed = { settings.defaultPlaybackSpeed }, autoPlayNext = { settings.autoPlayNext },
        loopMode = { settings.loopMode },
        sponsorSkipMode = { settings.sponsorSkipMode }, danmakuOn = { settings.danmakuOn },
        bluetoothLatency = { rememberPlayerBluetoothLatency() }, engine = engine,
        render = { playback, media, presentation, actions, ui ->
            rememberPlayerRenderPorts(playback, media, presentation, actions, ui)
        },
        hasNext = dependencies.queue::hasNext,
    )
}
