package com.aritxonly.teak.common.material

import com.aritxonly.teak.common.material.miuixControlFeedback
import com.aritxonly.teak.common.theme.LocalTeakControlEmphasis
import com.aritxonly.teak.common.theme.LocalTeakContentColors
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import com.aritxonly.teak.common.chrome.LocalProgressiveTopBarMaterialHosted
import com.aritxonly.teak.common.chrome.topBarMaterialAlphaProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import top.yukonga.miuix.kmp.blur.Backdrop

object TeakGlassDefaults {
    /** 仅供原本有平面底色的控件使用，跟随应用主题而非系统主题。 */
    @Composable
    fun flatBackgroundAlpha(): Float =
        if (MaterialTheme.colorScheme.onSurface.luminance() > 0.5f) 0.9f else 0.96f

    val Shape: Shape = Capsule()
    val IconButtonSize: Dp = 44.dp
}

/**
 * Layout-neutral high-level entry point for Deadliner's soft-glass material.
 *
 * The caller owns size and padding. This layer only resolves the active material, backdrop,
 * tint strategy and optical recipe before delegating to [teakGlassMaterial].
 */
@Composable
fun TeakGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = TeakGlassDefaults.Shape,
    recipe: GlassMaterialRecipe? = null,
    tint: Color? = null,
    tintStyle: GlassTintStyle = GlassTintStyle.Neutral,
    interaction: GlassMaterialInteraction = GlassMaterialInteraction(),
    materialAlpha: Float = 1f,
    /** 顶栏宿主内使用：alpha 在 draw 阶段读取，避免滚动渐显每帧重组。 */
    materialAlphaProvider: (() -> Float)? = topBarMaterialAlphaProvider(),
    /** Optional flat state, enabled only by home controls and segmented selectors. */
    flatBackgroundAlpha: Float = 0f,
    baseAlpha: Float = 1f,
    backdrop: Backdrop? = LocalTeakBackdrop.current,
    contentAlignment: Alignment = Alignment.Center,
    flatBackgroundColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    // 经典样式不使用玻璃：所有玻璃表面退化为平面容器，保留调用方指定的形状。
    if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
        Box(
            modifier = modifier.clip(shape).background(
                flatBackgroundColor ?: top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer,
            ),
            contentAlignment = contentAlignment,
        ) {
            content()
        }
        return
    }
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val fineTuning = advancedMaterial.fineTuning
    val resolvedRecipe = recipe ?: remember(fineTuning) {
        TeakGlassRecipes.floatingNavigation(fineTuning)
    }
    val resolvedTint = tint ?: advancedMaterial.fineTuning
        .softGlassTintColor(isDark)
        .copy(
            alpha = if (advancedMaterial.enabled) {
                advancedMaterial.navigationTintAlpha
            } else {
                0.92f
            },
        )

    // 平面底与玻璃必须使用同一份 draw 期 alpha 做交叉淡出，否则滚动渐显时平面态会直接消失。
    val resolvedAlpha: () -> Float = {
        (materialAlphaProvider?.invoke() ?: materialAlpha).coerceIn(0f, 1f)
    }
    val flatColor = flatBackgroundColor
        ?: top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer
    Box(
        modifier = modifier.clip(shape),
        contentAlignment = contentAlignment,
    ) {
        if (flatBackgroundAlpha > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = flatBackgroundAlpha.coerceIn(0f, 1f) * (1f - resolvedAlpha())
                    }
                    .background(flatColor, shape),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = resolvedAlpha()
                }
                .background(resolvedTint.copy(alpha = baseAlpha.coerceIn(0f, 1f)))
                .teakGlassMaterial(
                    advancedMaterial = advancedMaterial,
                    backdrop = backdrop,
                    shape = shape,
                    tint = resolvedTint,
                    tintStyle = tintStyle,
                    isDark = isDark,
                    recipe = resolvedRecipe,
                    interaction = interaction,
                ),
        )
        if (LocalTeakControlEmphasis.current && advancedMaterial.enabled) {
            val highlight = LocalTeakContentColors.current.glassHighlight
            // 宽而柔和的渐变光带，不沿轮廓描线，也不覆盖中性灰底。
            Box(Modifier.matchParentSize()
                .graphicsLayer { alpha = resolvedAlpha() }
                .background(Brush.linearGradient(
                0f to highlight.copy(alpha = if (isDark) 0.12f else 0.18f),
                0.34f to highlight.copy(alpha = 0f),
                0.78f to highlight.copy(alpha = 0f),
                1f to highlight.copy(alpha = if (isDark) 0.035f else 0.06f),
            )))
        }
        content()
    }
}

/** A circular soft-glass host for icon-only top-bar actions. */
@Composable
fun TeakGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = TeakGlassDefaults.IconButtonSize,
    recipe: GlassMaterialRecipe? = null,
    tint: Color? = null,
    tintStyle: GlassTintStyle = GlassTintStyle.Neutral,
    backdrop: Backdrop? = LocalTeakBackdrop.current,
    materialAlpha: Float = 1f,
    /** 顶栏宿主内使用：alpha 在 draw 阶段读取。 */
    materialAlphaProvider: (() -> Float)? = topBarMaterialAlphaProvider(),
    flatBackgroundAlpha: Float = 0f,
    neutralTintProgress: Float = 0f,
    content: @Composable () -> Unit,
) {
    // 经典样式：顶栏按钮只保留图标，不再画圆底。
    if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
        Box(
            modifier = modifier
                .size(size)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f

    val fineTuning = LocalTeakMaterialSpec.current.fineTuning
    val controlRecipe = remember(recipe, fineTuning) {
        (recipe ?: TeakGlassRecipes.floatingNavigation(fineTuning)).copy(surfaceRole = GlassSurfaceRole.Control)
    }

    // Match floating navigation: elevation belongs to the outer frame, not the glass clip.
    Box(
        modifier = modifier
            .size(size)
            .teakGlassShadow(
                shape = TeakGlassDefaults.Shape,
                isDark = isDark,
                alpha = materialAlpha,
                alphaProvider = materialAlphaProvider,
                profile = if (LocalTeakControlEmphasis.current) {
                    if (LocalProgressiveTopBarMaterialHosted.current) TeakGlassShadowProfile.ContentTopBarControl
                    else TeakGlassShadowProfile.ContentControl
                } else if (LocalProgressiveTopBarMaterialHosted.current) TeakGlassShadowProfile.TopBarControl
                    else TeakGlassShadowProfile.Standard,
            )
            .miuixControlFeedback(enabled = enabled, shape = TeakGlassDefaults.Shape),
    ) {
        TeakGlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = TeakGlassDefaults.Shape,
            recipe = controlRecipe,
            tint = tint,
            tintStyle = tintStyle,
            backdrop = backdrop,
            materialAlpha = materialAlpha,
            materialAlphaProvider = materialAlphaProvider,
            flatBackgroundAlpha = flatBackgroundAlpha,
            interaction = GlassMaterialInteraction(neutralTintProgress = neutralTintProgress),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
