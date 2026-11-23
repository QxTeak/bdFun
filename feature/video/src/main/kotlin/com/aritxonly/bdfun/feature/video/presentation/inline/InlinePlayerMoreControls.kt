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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule
import kotlin.math.abs
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.SearchDevice
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val HALF_SPEEDS = listOf(3f, 2f, 1.5f, 1.25f, 1f)

@Composable
internal fun HalfSpeedRow(speed: Float, onSelect: (Float) -> Unit, accentColor: Color) {

    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpeedGlyph(colors.onBackground)
        Spacer(Modifier.width(12.dp))
        Text("倍速", color = colors.onBackground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        HALF_SPEEDS.forEachIndexed { index, value ->
            if (index > 0) {
                Text(
                    text = "|",
                    color = colors.onBackgroundVariant.copy(alpha = 0.28f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Text(
                text = if (value % 1f == 0f) value.toInt().toString() else value.toString(),
                color = if (kotlin.math.abs(speed - value) < 0.01f) {
                    accentColor
                } else {
                    colors.onBackgroundVariant
                },
                // 半屏更多面板的倍速选项也比原来大一号，和全屏倍速面板一致。
                fontSize = 12.5.sp,
                modifier = Modifier.clickable(interactionSource = null, indication = null) {
                    onSelect(value)
                },
            )
        }
    }
}

@Composable
internal fun HalfQuickAction(
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            when (label) {
                "不感兴趣" -> DislikeGlyph(tint)
                "稍后再看" -> androidx.compose.material3.Icon(MiuixIcons.SearchDevice, null, Modifier.size(24.dp), tint = tint)
                "缓存" -> androidx.compose.material3.Icon(MiuixIcons.Download, null, Modifier.size(24.dp), tint = tint)
                "小窗播放" -> PipGlyphSmall(tint)
                else -> CastGlyphSmall(tint)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = label, color = tint.copy(alpha = 0.8f), fontSize = 10.5.sp)
    }
}

@Composable
internal fun HalfMoreSwitchRow(
    label: String,
    checked: Boolean,
    accentColor: Color,
    glyph: @Composable () -> Unit,
    onToggle: () -> Unit,
) {

    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph()
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = colors.onBackground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(22.dp)
                .clip(Capsule())
                .background(if (checked) accentColor else colors.onBackgroundVariant.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = if (checked) 20.dp else 2.dp, top = 2.dp)
                    .size(18.dp)
                    .clip(Capsule())
                    .background(Color.White),
            )
        }
    }
}

@Composable
internal fun HalfLoopRow(loopMode: Int, onSelect: (Int) -> Unit, accentColor: Color) {

    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoopGlyph(colors.onBackground)
        Spacer(Modifier.width(12.dp))
        Text("循环播放", color = colors.onBackground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        listOf("单集循环", "列表循环", "不循环").forEachIndexed { index, label ->
            if (index > 0) {
                Text(
                    text = "|",
                    color = colors.onBackgroundVariant.copy(alpha = 0.28f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 5.dp),
                )
            }
            val active = if (index == 2) loopMode == 0 else loopMode == index + 1
            Text(
                text = label,
                color = if (active) accentColor else colors.onBackgroundVariant,
                fontSize = 11.5.sp,
                modifier = Modifier.clickable(interactionSource = null, indication = null) {
                    onSelect(if (index == 2) 0 else index + 1)
                },
            )
        }
    }
}

@Composable
internal fun HalfMoreChoiceRow(
    label: String,
    value: String,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit,
) {

    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph()
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = colors.onBackground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        if (value.isNotBlank()) {
            Text(text = value, color = colors.onBackgroundVariant, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
        }
        AppGlyph(MiuixIcons.Basic.ArrowRight, size = 16.dp, tint = colors.onBackgroundVariant)
    }
}
