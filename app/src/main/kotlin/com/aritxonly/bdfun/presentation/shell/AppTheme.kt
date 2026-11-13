package com.aritxonly.bdfun


import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

/** HyperOS 色板：B 站主色 + Miuix 表面层级。 */
private val AcPink = Color(0xFFFB7299)
private val AcPinkDeep = Color(0xFFE45C82)
private val HyperBlue = Color(0xFF3482FF)
private val HyperBlueDeep = Color(0xFF1D63D8)

/** 色板不再复制一份全局style；读取本容器的设置，Compose观察同一个来源。 */
internal class AppThemePalette(private val currentStyle: () -> Int) {
    val brand: Color get() = if (currentStyle() == 1) HyperBlue else AcPink
    val brandDeep: Color get() = if (currentStyle() == 1) HyperBlueDeep else AcPinkDeep
}

internal val BrandPink: Color get() = AppGraph.themePalette?.brand ?: AcPink
internal val BrandPinkDeep: Color get() = AppGraph.themePalette?.brandDeep ?: AcPinkDeep

internal fun hyperLightColors() = miuixLightColorScheme(
    primary = BrandPink,
    onPrimary = Color.White,
    primaryVariant = BrandPink,
    onPrimaryVariant = Color(0xFF5A2735),
    secondaryVariant = BrandPink,
    onSecondaryVariant = Color.White,
    background = Color(0xFFF3F3F3),
    onBackground = Color(0xFF191919),
    onBackgroundVariant = Color(0xFF6F6F6F),
    surface = Color(0xFFF3F3F3),
    onSurface = Color(0xFF191919),
    surfaceVariant = Color(0xFFECECEC),
    onSurfaceSecondary = Color(0xFF191919).copy(alpha = 0.8f),
    onSurfaceVariantSummary = Color(0xFF6F6F6F),
    onSurfaceVariantActions = Color(0xFF191919),
    disabledOnSurface = Color(0xFF6F6F6F).copy(alpha = 0.5f),
    surfaceContainer = Color(0xFFFFFFFF),
    onSurfaceContainer = Color(0xFF191919),
    onSurfaceContainerVariant = Color(0xFF6F6F6F),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    onSurfaceContainerHigh = Color(0xFF191919),
    surfaceContainerHighest = Color(0xFFE8E8E8),
    onSurfaceContainerHighest = Color(0xFF191919),
    outline = Color(0xFFD9D9D9),
    dividerLine = Color(0xFFE0E0E0),
)

internal fun hyperDarkColors() = miuixDarkColorScheme(
    primary = BrandPink,
    onPrimary = Color.White,
    primaryVariant = BrandPinkDeep,
    onPrimaryVariant = Color(0xFFFFD9E3),
    secondaryVariant = BrandPinkDeep,
    onSecondaryVariant = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    onBackgroundVariant = Color(0xFFA6A6A6),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF333333),
    onSurfaceSecondary = Color(0xFFF2F2F2).copy(alpha = 0.8f),
    onSurfaceVariantSummary = Color(0xFFA6A6A6),
    onSurfaceVariantActions = Color(0xFFF2F2F2),
    disabledOnSurface = Color(0xFFA6A6A6).copy(alpha = 0.5f),
    surfaceContainer = Color(0xFF242424),
    onSurfaceContainer = Color(0xFFF2F2F2),
    onSurfaceContainerVariant = Color(0xFFA6A6A6),
    surfaceContainerHigh = Color(0xFF242424),
    onSurfaceContainerHigh = Color(0xFFF2F2F2),
    surfaceContainerHighest = Color(0xFF2D2D2D),
    onSurfaceContainerHighest = Color(0xFFF2F2F2),
    outline = Color(0xFF404040),
    dividerLine = Color(0xFF393939),
)

/** Material 控件与 Miuix 共用中性色，避免独立页面沿用旧色板。 */
internal fun appMaterialColors(dark: Boolean): androidx.compose.material3.ColorScheme {
    val colors = if (dark) hyperDarkColors() else hyperLightColors()
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = colors.primary,
        background = colors.background,
        onBackground = colors.onBackground,
        surface = colors.surface,
        onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.onBackgroundVariant,
        surfaceContainer = colors.surfaceContainer,
        surfaceContainerHigh = colors.surfaceContainerHigh,
        surfaceContainerHighest = colors.surfaceContainerHighest,
        outline = colors.outline,
        outlineVariant = colors.dividerLine,
    )
}
