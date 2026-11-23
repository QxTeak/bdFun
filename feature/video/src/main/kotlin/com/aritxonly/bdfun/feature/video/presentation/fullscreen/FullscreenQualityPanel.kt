package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun QualityPanel(
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
    accentColor: Color,
) {
    PanelCard(title = "画质") {
        if (playback.qualities.isEmpty()) {
            Text("暂无可用画质", color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            // 选项均分剩余高度，但每行自身保持固定高度：选中框不会跟着间距一起长高。
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            AutomaticQualityRow(playback.autoQuality, actions, onClose, accentColor, Modifier.height(OptionRowHeight))
            playback.qualities
                .let { list ->
                    // 和半屏播放器一致：不列 720P 以下的档位；
                    // 视频本身最高不到 720P 时退回完整列表，避免菜单空掉。
                    list.filter { it.id >= 64 }.ifEmpty { list }
                }
                .forEach { item ->
                    val active = !playback.autoQuality && item.id == playback.qualityId
                    QualityOptionRow(item, active, actions, onClose, accentColor, Modifier.height(OptionRowHeight))
                }
        }
    }
}

@Composable
private fun AutomaticQualityRow(
    active: Boolean,
    actions: FullscreenActions,
    onClose: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    // 自动：按当前网速挑一档；选中后画质标签显示「自动」。
    OptionRow(
        active = active,
        accentColor = accentColor,
        modifier = modifier,
        onClick = {
            actions.onQualitySelected(0)
            onClose()
        },
    ) {
        Text(
            text = "自动",
            color = if (active) accentColor else Color.White,
            fontSize = 16.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun QualityOptionRow(
    item: com.aritxonly.bdfun.core.model.video.Quality,
    active: Boolean,
    actions: FullscreenActions,
    onClose: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    // 只显示 1080P / 720P 这类标签，不再把实际分辨率写出来。
    OptionRow(
        active = active,
        accentColor = accentColor,
        modifier = modifier,
        onClick = {
            actions.onQualitySelected(item.id)
            onClose()
        },
    ) {
        Text(
            text = qualityMenuLabel(item.label),
            color = if (active) accentColor else Color.White,
            fontSize = 16.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun OptionRow(
    active: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) accentColor.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { content() }
        if (active) CheckGlyph(accentColor)
    }
}
