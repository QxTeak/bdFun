package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassRecipes
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward

/**
 * 页面级返回键：progress = 0 时只有箭头（无背景），滚出顶部后逐渐露出玻璃底。
 *
 * 按钮组件为 Teak 新写；玻璃背景和 recipe 直接复用 HyperPiliPlus 的玻璃材质实现。
 * 来源记录见 docs/PROVENANCE.md。
 *
 * `revealed` 为 true 时与底栏、顶栏使用同一套 recipe / 同一块 backdrop；
 * [light] = true 用于视频 / 评论页（背后是 SurfaceView，采不到画面），固定白色箭头。
 *
 * 必须由采集层之外的覆盖层绘制，否则 backdrop 会与当前页面形成 RenderNode 自引用。
 */
@Composable
fun TeakGlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = TeakChromeDefaults.BackButtonSize,
    revealed: Boolean = true,
    light: Boolean = false,
    /** 自定义箭头颜色（个人主页压在头图上时做黑白渐变用）。 */
    glyphColor: Color? = null,
    flatBackgroundAlpha: Float = 0f,
) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val glyph = glyphColor ?: if (light) Color.White else teakGlassGlyphColor()
    // 固定速率淡入/淡出（300ms），与页面滚动速度无关。
    val fallbackReveal = rememberManualProgress(if (revealed) 1f else 0f, 300f)
    // 经典顶栏常驻，返回键也常驻平面圆底，不随滚动淡入。
    val reveal = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 1f
        else if (LocalProgressiveTopBarMaterialHosted.current) LocalTopBarButtonMaterialProgress.current else fallbackReveal

    Box(modifier = modifier.size(size)) {
        val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
        if (classic) {
            // 经典顶栏：返回键只保留图标，不加任何圆底。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(interactionSource = null, indication = null, onClick = onClick),
            )
        } else if (reveal > 0.01f) {
            TeakGlassIconButton(
                onClick = onClick,
                size = size,
                recipe = recipe,
                backdrop = LocalTeakBackdrop.current,
                materialAlpha = reveal,
                flatBackgroundAlpha = flatBackgroundAlpha,
            ) {}
        }
        // 统一使用 Miuix 的 ChevronBackward 图标，替代原来手画的折线。
        Icon(
            imageVector = MiuixIcons.ChevronBackward,
            contentDescription = "返回",
            tint = glyph,
            modifier = Modifier
                .size(size * 0.552f)
                .align(Alignment.Center),
        )
        // 透明热区：没有背景时也能点。
        if (reveal <= 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer.copy(alpha = flatBackgroundAlpha.coerceIn(0f, 1f)), androidx.compose.foundation.shape.CircleShape)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(interactionSource = null, indication = null, onClick = onClick),
            )
        }
    }
}
