package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.feature.settings.application.*

@Composable
internal fun DanmakuPanelFilters(runtime: DanmakuPanelRuntime) {
    val settings = runtime.settings
    val onInteract = runtime.onInteract
    DmGroup(title = "画面防挡") {
        DmTypeFilters(settings)
        Spacer(Modifier.height(6.dp))
        if (!runtime.liveMode) {
            DmSwitchRow(
                title = "重复弹幕隐藏",
                description = "大量内容相同的弹幕会被隐藏",
                checked = settings.danmakuDedupe,
                onInteract = onInteract,
            ) { settings.danmakuDedupe = it }

            DmSwitchRow(
                title = "底部字幕区域防挡",
                description = "底部弹幕会被隐藏",
                checked = settings.danmakuSubtitleGuard,
                onInteract = onInteract,
            ) { settings.danmakuSubtitleGuard = it }
        }
    }
}

@Composable
internal fun DmTypeFilters(settings: SettingsBindings) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = 6.dp
        val tile = ((maxWidth - gap * 4) / 5).coerceAtLeast(34.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            DmChip("固定", DmIconKind.Fixed, !settings.danmakuBlockFixed, tile) {
                settings.danmakuBlockFixed = !settings.danmakuBlockFixed
            }
            DmChip("滚动", DmIconKind.Scroll, !settings.danmakuBlockScroll, tile) {
                settings.danmakuBlockScroll = !settings.danmakuBlockScroll
            }
            DmChip("彩色", DmIconKind.Color, !settings.danmakuBlockColor, tile) {
                settings.danmakuBlockColor = !settings.danmakuBlockColor
            }
            DmChip("高级", DmIconKind.Advanced, !settings.danmakuBlockAdvanced, tile) {
                settings.danmakuBlockAdvanced = !settings.danmakuBlockAdvanced
            }
            DmChip("计数", DmIconKind.Count, !settings.danmakuBlockCount, tile) {
                settings.danmakuBlockCount = !settings.danmakuBlockCount
            }
        }
    }
}

@Composable
internal fun DmChip(
    label: String,
    kind: DmIconKind,
    active: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val palette = LocalDanmakuPanelPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(size)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    if (active) palette.chipActive else palette.chip,
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            DmTypeGlyph(kind = kind, tint = if (active) palette.text else palette.muted)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) palette.text else palette.muted,
            fontSize = 10.sp,
        )
    }
}
