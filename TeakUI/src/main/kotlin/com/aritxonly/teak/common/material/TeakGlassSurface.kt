package com.aritxonly.teak.common.material

import com.aritxonly.teak.common.material.miuixControlFeedback
import com.aritxonly.teak.common.theme.LocalTeakControlEmphasis
import com.aritxonly.teak.common.theme.LocalTeakContentColors
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import com.aritxonly.teak.common.chrome.LocalProgressiveTopBarMaterialHosted
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
    materialAlpha: Float = if (LocalProgressiveTopBarMaterialHosted.current)
        com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current else 1f,
    /** Optional flat state, enabled only by home controls and segmented selectors. */
    flatBackgroundAlpha: Float = 0f,
    baseAlpha: Float = 1f,
    backdrop: Backdrop? = LocalTeakBackdrop.current,
    contentAlignment: Alignment = Alignment.Center,
    flatBackgroundColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val resolvedRecipe = recipe ?: TeakGlassRecipes.floatingNavigation(
        advancedMaterial.fineTuning,
    )
    val resolvedTint = tint ?: advancedMaterial.fineTuning
        .softGlassTintColor(isDark)
        .copy(
            alpha = if (advancedMaterial.enabled) {
                advancedMaterial.navigationTintAlpha
            } else {
                0.92f
            },
        )

    Box(
        modifier = modifier.clip(shape).background(
            (flatBackgroundColor ?: top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer).copy(
                alpha = flatBackgroundAlpha.coerceIn(0f, 1f) * (1f - materialAlpha.coerceIn(0f, 1f)),
            ),
        ),
        contentAlignment = contentAlignment,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = materialAlpha.coerceIn(0f, 1f)
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
                .graphicsLayer { alpha = materialAlpha.coerceIn(0f, 1f) }
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
    materialAlpha: Float = if (LocalProgressiveTopBarMaterialHosted.current)
        com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current else 1f,
    flatBackgroundAlpha: Float = 0f,
    neutralTintProgress: Float = 0f,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f

    // Match floating navigation: elevation belongs to the outer frame, not the glass clip.
    Box(
        modifier = modifier
            .size(size)
            .teakGlassShadow(
                shape = TeakGlassDefaults.Shape,
                isDark = isDark,
                alpha = materialAlpha,
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
            recipe = (recipe ?: TeakGlassRecipes.floatingNavigation(
                LocalTeakMaterialSpec.current.fineTuning,
            )).copy(surfaceRole = GlassSurfaceRole.Control),
            tint = tint,
            tintStyle = tintStyle,
            backdrop = backdrop,
            materialAlpha = materialAlpha,
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
