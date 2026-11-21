package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/** 弹幕互动卡片的投票行、评分胶囊与图标按钮；从 Overlay 拆出，保持单文件尺寸。 */
@Composable
internal fun VoteOptionRow(
    label: String,
    selected: Boolean,
    showRatio: Boolean,
    ratio: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    // 投票选项统一胶囊形状；比例条也用同一胶囊，按真实票数占比填充。
    val shape = Capsule()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = if (selected) 0.16f else 0.07f))
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                color = Color.White.copy(alpha = if (selected) 0.9f else 0.3f),
                shape = shape,
            )
            .clickable(enabled = enabled, interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (showRatio && ratio > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(1.dp)
                    .clip(shape)
                    .fillMaxWidth(ratio)
                    // 透明粉：Miuix 主题粉 + 低不透明度，不抢白色文字。
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.32f)),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (selected) "✓ $label" else label,
                color = Color.White.copy(alpha = if (enabled || selected) 0.95f else 0.6f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showRatio) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${(ratio * 100).roundToInt()}%",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
internal fun RowScope.CapsuleOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = Capsule()
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (selected) 0.95f else 0.42f),
                shape = shape,
            )
            .clickable(enabled = enabled, interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (selected) "✓ $label" else label,
            color = Color.White.copy(alpha = if (enabled || selected) 0.92f else 0.45f),
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun CapsuleButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = Capsule()
    Box(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.6f else 0.3f), shape)
            .clickable(enabled = enabled, interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White.copy(alpha = if (enabled) 0.92f else 0.45f), fontSize = 11.5.sp, maxLines = 1)
    }
}

@Composable
internal fun CapsuleIconButton(
    icon: Int,
    tint: Color,
    description: String,
    onClick: () -> Unit,
) {
    CapsuleIconBox(onClick = onClick) {
        AppGlyph(icon, tint, size = 15.dp, contentDescription = description)
    }
}

@Composable
internal fun CapsuleIconButton(
    icon: ImageVector,
    tint: Color,
    description: String,
    onClick: () -> Unit,
) {
    CapsuleIconBox(onClick = onClick) {
        AppGlyph(icon, tint, size = 15.dp, contentDescription = description)
    }
}

@Composable
private fun CapsuleIconBox(onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = Capsule()
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(shape)
            .border(0.5.dp, Color.White.copy(alpha = 0.3f), shape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
