package com.aritxonly.teak.common.chrome

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 页内玻璃控件（返回键 / 搜索条 / 消息键通用）。
 *
 * 刻意不接底栏那套 `LayerBackdrop`：`layerBackdrop` 录制的正是"包含这些控件的页面内容"，
 * 页内控件再去读它就会让 RenderNode 互相引用（hwui 无限递归 → 闪退）。
 * 所以这里直接画：半透明竖向渐变底 + 上亮下暗的 1dp 高光描边 + 柔和投影 + 按压回弹。
 * 观感与底栏一致（同一套色值/描边/阴影语言），只是不做 backdrop 模糊。
 */
@Composable
fun TeakGlass(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "teak_glass_press",
    )

    // 玻璃体：上亮下略灰的竖向渐变，白底上也能看出"有厚度"，不是一块死白
    val fill = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xF233353C), Color(0xEB1B1C21)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFAFFFFFF), Color(0xE6E9ECF1)))
    }
    // 描边：上缘高光 → 下缘略深，形成玻璃边缘的那道亮/暗线
    val rim = if (isDark) {
        Brush.verticalGradient(listOf(Color(0x47FFFFFF), Color(0x0DFFFFFF)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x80AEB4BF)))
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.14f),
            )
            .clip(shape)
            .background(fill)
            // 描边必须挂在同一条链上：放到子 Box 里用 fillMaxSize 会把父 Box 撑到最大约束
            .border(width = 1.dp, brush = rim, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}

/** 玻璃控件里的图标色：浅色近黑、深色近白，与底栏图标同源。 */
@Composable
fun teakGlassGlyphColor(): Color {
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    return if (isDark) Color(0xFFF2F3F5) else Color(0xFF1B1C1F)
}
