package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.modal.TeakActionSheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HalfScreenMoreSheet(
    visible: Boolean, onDismiss: () -> Unit,
    state: HalfMoreState, quick: HalfMoreQuickActions, settings: HalfMoreSettingsActions, accentColor: androidx.compose.ui.graphics.Color,
) {

    val now = System.currentTimeMillis()
    val timerMinutes = if (state.sleepAtMs > now) {
        ((state.sleepAtMs - now + 59_999L) / 60_000L).toInt()
    } else {
        0
    }
    TeakActionSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "更多",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            HalfMoreQuickRow(onDismiss, quick)
            Spacer(Modifier.height(6.dp))
            HalfMorePlaybackSettings(state, settings, timerMinutes, accentColor)
            HalfMoreExtraSettings(state, settings, accentColor)
        }
    }
}


@Composable
private fun HalfMoreQuickRow(onDismiss: () -> Unit, quick: HalfMoreQuickActions) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        HalfQuickAction("不感兴趣", colors.onBackground) {
            quick.onDislike()
            onDismiss()
        }
        HalfQuickAction("稍后再看", colors.onBackground) {
            quick.onWatchLater()
            onDismiss()
        }
        HalfQuickAction("缓存", colors.onBackground) {
            quick.onCache()
            onDismiss()
        }
        HalfQuickAction("小窗播放", colors.onBackground) {
            quick.onPictureInPicture()
            onDismiss()
        }
        HalfQuickAction("投屏", colors.onBackground) {
            quick.onCast()
            onDismiss()
        }
    }
}

@Composable
private fun HalfMorePlaybackSettings(state: HalfMoreState, settings: HalfMoreSettingsActions, timerMinutes: Int, accentColor: androidx.compose.ui.graphics.Color) {
    val colors = MiuixTheme.colorScheme
    HalfSpeedRow(
        speed = state.speed,
        accentColor = accentColor,
        onSelect = settings.onSpeed,
    )
    HalfMoreSwitchRow(
        accentColor = accentColor,
        label = "自动连播",
        checked = state.autoPlayNext,
        glyph = { AutoPlayNextGlyph(colors.onBackground) },
        onToggle = settings.onAutoPlayNext,
    )
    HalfLoopRow(loopMode = state.loopMode, onSelect = settings.onLoopMode, accentColor = accentColor)
    HalfMoreChoiceRow(
        label = "定时关闭",
        value = if (timerMinutes > 0) "${timerMinutes}分钟" else "不开启",
        glyph = { TimerGlyph(colors.onBackground) },
    ) {
        val next = when {
            timerMinutes <= 0 -> 15
            timerMinutes <= 15 -> 30
            timerMinutes <= 30 -> 60
            else -> 0
        }
        settings.onSleepAt(if (next == 0) 0L else System.currentTimeMillis() + next * 60_000L)
    }
}

@Composable
private fun HalfMoreExtraSettings(
    state: HalfMoreState, settings: HalfMoreSettingsActions,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    val colors = MiuixTheme.colorScheme
    HalfMoreSwitchRow(
        accentColor = accentColor,
        label = "后台听视频",
        checked = state.backgroundPlay,
        glyph = { BackgroundListenGlyph(colors.onBackground) },
        onToggle = settings.onBackgroundPlay,
    )
    HalfMoreSwitchRow(
        accentColor = accentColor,
        label = "字幕",
        checked = state.subtitlesOn,
        glyph = { SubtitleGlyph(colors.onBackground) },
        onToggle = settings.onSubtitlesToggle,
    )
    HalfMoreChoiceRow(
        label = "弹幕设置",
        value = "",
        glyph = { DanmakuSettingGlyph(colors.onBackground) },
    ) {
        // 叠一层新的 sheet，「更多」先留在下面，关掉弹幕设置就回到这里。
        settings.onDanmakuSettings()
    }
    HalfMoreSwitchRow(
        accentColor = accentColor,
        label = "镜像翻转",
        checked = state.mirror,
        glyph = { MirrorGlyph(colors.onBackground) },
        onToggle = settings.onMirror,
    )
}
