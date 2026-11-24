package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackSnapshot

/** 共享只读投影；事件闭包持有 State 委托，读取的是触发时的播放器状态。 */
class PlayerObservedStateHolder(snapshot: State<PlaybackSnapshot>) {
    val playing = derivedStateOf { snapshot.value.playing }
    val ended = derivedStateOf { snapshot.value.ended }
    val positionMs = derivedStateOf { snapshot.value.positionMs }
    val bufferedMs = derivedStateOf { snapshot.value.bufferedMs }
    val durationMs = derivedStateOf { snapshot.value.durationMs }
    val videoWidth = derivedStateOf { snapshot.value.videoWidth }
    val videoHeight = derivedStateOf { snapshot.value.videoHeight }
    /** 竖屏视频（高大于宽）进全屏时保持竖屏铺满，而不是强制横屏。 */
    val portraitVideo = derivedStateOf {
        val w = snapshot.value.videoWidth
        val h = snapshot.value.videoHeight
        w > 0 && h > 0 && h > w
    }
}

@Composable
fun rememberPlayerObservedState(playback: PlaybackController): PlayerObservedStateHolder {
    val snapshot = playback.state.collectAsState()
    return remember(playback) { PlayerObservedStateHolder(snapshot) }
}
