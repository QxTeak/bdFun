package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.layout.BoxWithConstraints

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoItem

@Composable
fun HomeScreen(state: HomeFeedState, onOpenVideo: (VideoItem) -> Unit, cardStyle: Int = 0, host: HomePresentationHost, interactive: Boolean = true) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val ui = remember { HomePresentationStateHolder(state.refreshRequest) }
    val pull = HomePullSession(state, ui, density, with(density) { 88.dp.toPx() }, with(density) { 260.dp.toPx() })
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    HomeInstanceEffect(host)
    HomeFeedEffects(state, ui, pull)
    HomeExposureEffect(state, interactive && ui.actionVideo == null)
    val refreshConnection = remember(state) { pull.connection() }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        HomeVideoGrid(state, cardStyle == 1, maxWidth, statusBarTop, ui, refreshConnection, scope, onOpenVideo, host)
        HomeFeedStatus(state, scope, host)
    }
    HomeVideoActions(video = ui.actionVideo, state = state, onDismiss = { ui.actionVideo = null }, onMessage = host.onMessage)
}

@Composable
private fun HomeInstanceEffect(host: HomePresentationHost, interactive: Boolean = true) {
    androidx.compose.runtime.DisposableEffect(Unit) {
        host.onEnter()
        onDispose {
            host.onExit()
        }
    }
}
