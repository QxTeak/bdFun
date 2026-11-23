package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.feature.settings.application.*
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.compositionLocalOf

/**
 * 弹幕设置原本只画在播放器的深色面板上（全屏/直播/半屏），所以文案一直是白色。
 * 现在同一份内容也会放进主题化 sheet，必须能跟随浅色/深色主题；
 * 用调色板区分这两种落点，默认保持原来的深色面板配色。
 */
internal data class DanmakuPanelPalette(
    val text: Color,
    val secondary: Color,
    val muted: Color,
    val card: Color,
    val chip: Color,
    val chipActive: Color,
    val switchOff: Color,
    val track: Color,
    val tick: Color,
    val thumb: Color,
    val arrow: Color,
)

internal val DarkDanmakuPanelPalette = DanmakuPanelPalette(
    text = Color.White,
    secondary = Color(0xFFB9BDC6),
    muted = Color.White.copy(alpha = 0.45f),
    card = Color.White.copy(alpha = 0.055f),
    chip = Color.White.copy(alpha = 0.04f),
    chipActive = Color.White.copy(alpha = 0.10f),
    switchOff = Color.White.copy(alpha = 0.16f),
    track = Color.White.copy(alpha = 0.18f),
    tick = Color.White.copy(alpha = 0.35f),
    thumb = Color.White,
    arrow = Color.White.copy(alpha = 0.4f),
)

internal val LocalDanmakuPanelPalette = compositionLocalOf { DarkDanmakuPanelPalette }

/** 主题化面板用：文字/轨道/圆钮都取当前 Miuix 配色，浅色主题下也是深字。 */
@Composable
internal fun themedDanmakuPanelPalette(): DanmakuPanelPalette {
    val colors = MiuixTheme.colorScheme
    return DanmakuPanelPalette(
        text = colors.onBackground,
        secondary = colors.onBackgroundVariant,
        muted = colors.onBackgroundVariant.copy(alpha = 0.75f),
        card = colors.onBackground.copy(alpha = 0.05f),
        chip = colors.onBackground.copy(alpha = 0.035f),
        chipActive = colors.onBackground.copy(alpha = 0.08f),
        switchOff = colors.onBackground.copy(alpha = 0.16f),
        track = colors.onBackground.copy(alpha = 0.16f),
        tick = colors.onBackground.copy(alpha = 0.35f),
        thumb = colors.onBackground,
        arrow = colors.onBackgroundVariant.copy(alpha = 0.7f),
    )
}

internal val DmText: Color
    @Composable get() = LocalDanmakuPanelPalette.current.text
internal val DmSecondary: Color
    @Composable get() = LocalDanmakuPanelPalette.current.secondary
internal val DmMuted: Color
    @Composable get() = LocalDanmakuPanelPalette.current.muted
internal val DmPink = BrandPink

@Composable
internal fun DmGroup(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val palette = LocalDanmakuPanelPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = palette.text.copy(alpha = 0.92f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.card, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            content = content,
        )
    }
}

@Composable
internal fun DmSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onInteract: () -> Unit,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) {
                onInteract()
                onChange(!checked)
            }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = DmText, fontSize = 14.sp)
            if (description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(text = description, color = DmMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        DmSwitch(checked = checked)
    }
}

@Composable
internal fun DmSwitch(checked: Boolean) {
    val palette = LocalDanmakuPanelPalette.current
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(22.dp)
            .background(
                if (checked) DmPink else palette.switchOff,
                RoundedCornerShape(11.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(start = if (checked) 20.dp else 2.dp, top = 2.dp)
                .size(18.dp)
                .background(palette.thumb, RoundedCornerShape(9.dp)),
        )
    }
}

@Composable
internal fun DmActionRow(
    title: String,
    trailing: String = "",
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = DmText, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        if (trailing.isNotBlank()) {
            Text(text = trailing, color = DmSecondary, fontSize = 11.5.sp)
            Spacer(Modifier.width(6.dp))
        }
        AppGlyph(MiuixIcons.Basic.ArrowRight, size = 16.dp,
            tint = LocalDanmakuPanelPalette.current.arrow)
    }
}

internal fun speedWord(speed: Float): String = when {
    speed < 0.75f -> "极慢"
    speed < 0.95f -> "慢"
    speed <= 1.05f -> "正常"
    speed < 1.5f -> "快"
    else -> "极快"
}
