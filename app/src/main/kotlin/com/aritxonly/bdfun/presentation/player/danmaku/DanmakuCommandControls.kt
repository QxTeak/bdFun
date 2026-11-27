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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
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
    // 投票选项：参考官方投票弹幕——圆角矩形、左标题右百分比，选中项用主题粉底。
    val shape = RoundedCornerShape(8.dp)
    val accent = MiuixTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(if (selected && !showRatio) accent.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.08f))
            .drawBehind {
                if (showRatio && ratio > 0f) {
                    // 比例就是填充长度：50% 填一半，100% 填满；直接画在圆角裁剪内。
                    drawRect(
                        color = accent.copy(alpha = if (selected) 0.62f else 0.42f),
                        size = androidx.compose.ui.geometry.Size(
                            width = size.width * ratio.coerceIn(0f, 1f),
                            height = size.height,
                        ),
                    )
                }
            }
            .clickable(enabled = enabled, interactionSource = null, indication = null, onClick = onClick),
    ) {
        Row(
            // 每个选项上下收窄一点，整张投票卡不再显得又高又满。
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selected) "✓ $label" else label,
                color = Color.White.copy(alpha = if (enabled || selected) 0.95f else 0.72f),
                fontSize = 13.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showRatio) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${(ratio * 100).roundToInt()}%",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * 评分投票：五颗星，点第几颗就前几颗变黄。
 *
 * 未选中是播放器空心星（fs_star），选中后填成黄色的实心星（act_star），前后同一形状。
 */
@Composable
internal fun GradeStarRow(
    options: List<com.aritxonly.bdfun.core.model.video.DanmakuCommandOption>,
    selected: Int?,
    enabled: Boolean,
    onClick: (Int) -> Unit,
) {
    val selectedIndex = options.indexOfFirst { it.value == selected }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val filled = selectedIndex >= 0 && index <= selectedIndex
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = enabled && selected == null,
                        interactionSource = null,
                        indication = null,
                    ) { onClick(option.value) },
                contentAlignment = Alignment.Center,
            ) {
                // 评分用 B 站自己的两颗星，前后形状一致：空心用播放器星，选中用实心星。
                // 不要再用 Miuix 的 FavoritesFill，那颗是心形。
                if (filled) {
                    AppGlyph(
                        icon = TeakIcon.Star,
                        tint = GradeStarColor,
                        size = 23.dp,
                        contentDescription = option.label,
                    )
                } else {
                    AppGlyph(
                        icon = TeakIcon.PlayerStar,
                        tint = Color.White.copy(alpha = 0.55f),
                        size = 23.dp,
                        contentDescription = option.label,
                    )
                }
            }
        }
    }
}

/** 评分选中的黄色。 */
private val GradeStarColor = Color(0xFFFFC63C)

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
