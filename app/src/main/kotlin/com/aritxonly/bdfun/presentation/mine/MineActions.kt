package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.icons.ResourceGlyph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.SearchDevice
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.feature.mine.application.*

internal val MineBlue = Color(0xFF3C9BD9)

@Composable
internal fun MineQuickActions(cardShape: androidx.compose.ui.graphics.Shape, navigation: MineNavigation) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surfaceContainer)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuickAction("离线缓存", Modifier.weight(1f), onClick = navigation.onOpenCache) {
            androidx.compose.material3.Icon(MiuixIcons.Download, null, Modifier.size(26.dp), tint = MineBlue)
        }
        QuickAction("历史记录", Modifier.weight(1f), onClick = navigation.onOpenHistory) {
            androidx.compose.material3.Icon(MiuixIcons.Recent, null, Modifier.size(26.dp), tint = MineBlue)
        }
        QuickAction("我的收藏", Modifier.weight(1f), onClick = navigation.onOpenLibrary) {
            ResourceGlyph(res = TeakIcon.Star, tint = MineBlue, size = 26.dp)
        }
        QuickAction("稍后再看", Modifier.weight(1f), onClick = navigation.onOpenWatchLater) {
            androidx.compose.material3.Icon(MiuixIcons.SearchDevice, null, Modifier.size(26.dp), tint = MineBlue)
        }
    }
}

@Composable
internal fun StatCell(label: String, value: Long, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatCount(value),
            color = colors.onBackground,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(text = label, color = colors.onBackgroundVariant, fontSize = 11.sp)
    }
}

@Composable
private fun QuickAction(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier.clickable(interactionSource = null, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(11.dp))
        Text(text = label, color = colors.onBackground, fontSize = 11.5.sp)
    }
}

@Composable
internal fun MineRow(
    text: String,
    color: Color? = null,
    trailingText: String = "＞",
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = color ?: colors.onBackground,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Text(text = trailingText, color = colors.outline, fontSize = 12.sp)
    }
}
