package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.extended.SearchDevice
import top.yukonga.miuix.kmp.icon.MiuixIcons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MorePanel(
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
    accentColor: Color,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        PanelHeader("更多", onClose)
        Spacer(Modifier.height(14.dp))
        FullscreenMoreQuickActions(actions, onClose)
        Spacer(Modifier.height(14.dp))
        FullscreenMoreSettings(playback, actions, accentColor)
        Spacer(Modifier.height(10.dp))
    }
}


@Composable
private fun FullscreenMoreQuickActions(actions: FullscreenActions, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        QuickAction("听视频", Modifier.weight(1f), { HeadphoneGlyph() }) {
            // 和半屏的听视频按钮一致：进入听视频时顺便打开后台播放。
            actions.onBackgroundPlayChanged(true)
            actions.onAudioOnlyChanged(true)
            onClose()
        }
        QuickAction("稍后再看", Modifier.weight(1f), { androidx.compose.material3.Icon(MiuixIcons.SearchDevice, null, Modifier.size(24.dp), tint = Color.White) }) {
            actions.onAddWatchLater()
            onClose()
        }
        QuickAction("小窗播放", Modifier.weight(1f), { PipGlyph() }) {
            actions.onPictureInPicture()
            onClose()
        }
        QuickAction("投屏", Modifier.weight(1f), { CastGlyph() }) {
            actions.onCast()
            onClose()
        }
    }
}

@Composable
private fun FullscreenMoreSettings(playback: FullscreenPlaybackState, actions: FullscreenActions, accentColor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MoreIconSwitchRow(
            label = "自动连播",
            accentColor = accentColor,
            checked = playback.autoPlayNext,
            glyph = { AutoPlayNextGlyph() },
        ) {
            actions.onAutoPlayNextChanged(!playback.autoPlayNext)
        }
        MoreLoopRow(
            loopMode = playback.loopMode,
            accentColor = accentColor,
            onSelect = { actions.onLoopModeChanged(it) },
        )
        MoreIconSwitchRow(
            label = "镜像翻转",
            accentColor = accentColor,
            checked = playback.mirror,
            glyph = { MirrorGlyph() },
        ) {
            actions.onMirrorChanged(!playback.mirror)
        }
        MoreIconSwitchRow(
            label = "后台听视频",
            accentColor = accentColor,
            checked = playback.backgroundPlay,
            glyph = { BackgroundListenGlyph() },
        ) {
            actions.onBackgroundPlayChanged(!playback.backgroundPlay)
        }
        MoreIconChoiceRow(
            label = "画面尺寸",
            value = when (playback.resizeMode) {
                1 -> "填充"
                2 -> "裁剪"
                else -> "适应"
            },
            glyph = { ResizeGlyph() },
        ) {
            actions.onResizeModeChanged((playback.resizeMode + 1) % 3)
        }
        MoreIconChoiceRow(
            label = "色觉辅助",
            value = when (playback.colorAssist) {
                1 -> "灰度"
                2 -> "高对比"
                else -> "不开启"
            },
            glyph = { ColorAssistGlyph() },
        ) {
            actions.onColorAssistChanged((playback.colorAssist + 1) % 3)
        }
    }
}
