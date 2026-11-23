package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.rememberManualProgress
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
internal fun FullscreenPanelHost(
    panel: FullscreenPanel,
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
    visible: Boolean = true,
    accentColor: Color,
    danmakuSettings: @Composable (List<com.aritxonly.bdfun.core.model.video.DanmakuItem>) -> Unit,
) {

    val ui = remember { FullscreenPanelHostStateHolder(panel) }
    var shown by ui.shown
    var revealed by ui.revealed
    LaunchedEffect(visible) { revealed = visible }
    val reveal = rememberManualProgress(if (revealed) 1f else 0f, 264f)
    LaunchedEffect(panel) {
        if (panel != FullscreenPanel.None) shown = panel
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val panelWidth = (maxWidth.value * 0.41f).dp.coerceAtLeast(360.dp)
        // 左透明、右深黑：与参考图三一致，不铺满一块纯黑
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.25f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.25f),
                        0.58f to Color.Black.copy(alpha = 0.74f),
                        0.68f to Color.Black.copy(alpha = 0.95f),
                        1f to Color.Black,
                    ),
                )
                .graphicsLayer { alpha = reveal }
                .clickable(interactionSource = null, indication = null, onClick = onClose),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(panelWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = reveal
                    translationX = (1f - reveal) * 90f
                }
                .clickable(interactionSource = null, indication = null) {}
                .padding(start = 32.dp, end = 30.dp, top = 30.dp, bottom = 18.dp),
        ) {
            when (shown) {
                FullscreenPanel.Favorite -> FavoritePanel(info, actions, onClose, accentColor)
                FullscreenPanel.More -> MorePanel(info, playback, actions, onClose, accentColor)
                FullscreenPanel.Danmaku -> DanmakuPanel(info, onClose, danmakuSettings)
                FullscreenPanel.Episodes -> EpisodesPanel(info, actions, onClose, accentColor)
                FullscreenPanel.Speed -> SpeedPanel(playback, actions, onClose, accentColor)
                FullscreenPanel.Quality -> QualityPanel(playback, actions, onClose, accentColor)
                FullscreenPanel.Cache -> CachePanel(playback, actions, onClose, accentColor)
                FullscreenPanel.Subtitles -> SubtitlesPanel(info, actions, onClose, accentColor)
                FullscreenPanel.None -> Unit
            }
        }
    }
}

private class FullscreenPanelHostStateHolder(initial: FullscreenPanel) {
    val shown = mutableStateOf(initial)
    val revealed = mutableStateOf(false)
}
