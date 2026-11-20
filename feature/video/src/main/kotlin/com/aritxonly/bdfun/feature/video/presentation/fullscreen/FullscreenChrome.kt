package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun FullscreenPlayerChrome(
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    panel: FullscreenPanel,
    onPanelChanged: (FullscreenPanel) -> Unit,
    modifier: Modifier = Modifier,
    scrubOnly: Boolean = false,
    scrub: FullscreenScrubActions = FullscreenScrubActions({}, {}, {}, {}),
    onPanelHidden: () -> Unit = {},
    appearance: FullscreenAppearance,
    slots: FullscreenChromeSlots,
) {

    if (playback.audioOnly) {
        FullscreenAudioMode(info = info, playback = playback,
            onBack = { actions.onAudioOnlyChanged(false) }, onPlayPause = actions.onPlayPause,
            onSeek = actions.onSeek, onSeekRelative = actions.onSeekRelative,
            modifier = modifier, appearance = appearance, slots = slots)
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val ui = rememberFullscreenPanelState(panel, onPanelHidden)
        val lastPanel by ui.lastPanel
        val panelVisible by ui.panelVisible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (!panelVisible && !scrubOnly) 1f else 0f
                },
        ) {
        // 竖屏视频全屏时屏幕是窄的，控件整体收缩并收起次要文字按钮，避免挤成一团
        val compact = com.aritxonly.bdfun.core.domain.isVerticalVideoAspect(info.videoAspectRatio)
        FullscreenChromeMasks(appearance, compact)
        FullscreenHeader(info, actions, onPanelChanged, appearance, compact)
        FullscreenAuthor(info, actions, appearance, slots, compact)
        FullscreenTimeline(playback, actions, scrub, compact)

        FullscreenBottomControls(info, playback, actions, onPanelChanged, appearance, compact)
        }

        // 二级面板：统一右侧渐变遮罩
        if (panelVisible && lastPanel != FullscreenPanel.None) {
            FullscreenPanelHost(
                panel = lastPanel,
                info = info,
                playback = playback,
                actions = actions,
                onClose = { onPanelChanged(FullscreenPanel.None) },
                visible = panel != FullscreenPanel.None,
                accentColor = appearance.accentColor,
                danmakuSettings = slots.danmakuSettings,
            )
        }
    }
}


class FullscreenScrubActions(
    val onScrubStart: () -> Unit, val onScrubEnd: () -> Unit,
    val onScrubLook: (Float) -> Unit, val onScrubPreview: (Long) -> Unit,
)

private class FullscreenPanelPresentationStateHolder(initial: FullscreenPanel) {
    val lastPanel = mutableStateOf(initial)
    val panelVisible = mutableStateOf(initial != FullscreenPanel.None)
}

@Composable
private fun rememberFullscreenPanelState(
    panel: FullscreenPanel, onPanelHidden: () -> Unit,
): FullscreenPanelPresentationStateHolder {
    val ui = remember { FullscreenPanelPresentationStateHolder(panel) }
    var lastPanel by ui.lastPanel
    var panelVisible by ui.panelVisible
    LaunchedEffect(panel) {
        if (panel != FullscreenPanel.None) {
            lastPanel = panel
            panelVisible = true
        } else if (panelVisible) {
            delay(264L)
            panelVisible = false
            onPanelHidden()
        }
    }
    return ui
}
