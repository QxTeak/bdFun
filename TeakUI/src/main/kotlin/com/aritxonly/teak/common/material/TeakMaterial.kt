package com.aritxonly.teak.common.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.aritxonly.teak.common.material.TeakMaterialFineTuning
import com.aritxonly.teak.common.material.TeakMaterialTuning
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.BlurDefaults.blurColors
import top.yukonga.miuix.kmp.blur.textureBlur

@Immutable
data class TeakMaterialSpec(
    val useOfficialMiuixGlass: Boolean = false,
    val shadowPreset: GlassShadowPreset = GlassShadowPreset.Default,
    val enabled: Boolean = false,
    val blurEnabled: Boolean = true,
    val blurRadius: Float = TeakMaterialTuning.DefaultBlurRadius,
    val blurRadiusX: Float? = null,
    val blurRadiusY: Float? = null,
    val noiseCoefficient: Float = TeakMaterialFineTuning.Default.noiseCoefficient,
    val blurBlendColors: List<BlendColorEntry> = emptyList(),
    val blurBrightness: Float = TeakMaterialFineTuning.Default.brightness,
    val blurContrast: Float = TeakMaterialFineTuning.Default.contrast,
    val blurSaturation: Float = TeakMaterialTuning.saturationForBlurRadius(
        TeakMaterialTuning.DefaultBlurRadius,
    ) * TeakMaterialFineTuning.Default.saturationMultiplier,
    val blurContentBlendMode: BlendMode = BlendMode.SrcOver,
    val navigationTintAlpha: Float = TeakMaterialFineTuning.Default.navigationTintAlpha,
    val topBarTintAlpha: Float = TeakMaterialFineTuning.Default.topBarTintAlpha,
    val fineTuning: TeakMaterialFineTuning = TeakMaterialFineTuning.Default,
) {
    val resolvedBlurRadius: Float
        get() = blurRadius.coerceIn(0f, BlurDefaults.MaxBlurRadius)

    val resolvedBlurRadiusX: Float
        get() = (blurRadiusX ?: blurRadius).coerceIn(0f, BlurDefaults.MaxBlurRadius)

    val resolvedBlurRadiusY: Float
        get() = (blurRadiusY ?: blurRadius).coerceIn(0f, BlurDefaults.MaxBlurRadius)

    val usesIndependentBlurRadii: Boolean
        get() = blurRadiusX != null || blurRadiusY != null

    val textureBlurEnabled: Boolean
        get() = enabled && blurEnabled
}

fun createTeakMaterialSpec(
    enabled: Boolean,
    blurRadius: Float,
    fineTuning: TeakMaterialFineTuning,
): TeakMaterialSpec {
    val tuning = fineTuning.normalized()
    return TeakMaterialSpec(
        enabled = enabled,
        blurRadius = TeakMaterialTuning.normalizeBlurRadius(blurRadius),
        noiseCoefficient = tuning.noiseCoefficient,
        blurBrightness = tuning.brightness,
        blurContrast = tuning.contrast,
        blurSaturation = TeakMaterialTuning.saturationForBlurRadius(blurRadius) *
            tuning.saturationMultiplier,
        navigationTintAlpha = tuning.navigationTintAlpha,
        topBarTintAlpha = tuning.topBarTintAlpha,
        fineTuning = tuning,
    )
}

@Composable
fun TeakMaterialSpec.rememberBlurColors(
    additionalBlendColors: List<BlendColorEntry> = emptyList(),
): BlurColors = blurColors(
    blendColors = additionalBlendColors + blurBlendColors,
    brightness = blurBrightness,
    contrast = blurContrast,
    saturation = blurSaturation,
)

fun Modifier.advancedTextureBlur(
    advancedMaterial: TeakMaterialSpec,
    backdrop: Backdrop,
    shape: Shape,
    colors: BlurColors,
): Modifier = if (advancedMaterial.usesIndependentBlurRadii) {
    textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadiusX = advancedMaterial.resolvedBlurRadiusX,
        blurRadiusY = advancedMaterial.resolvedBlurRadiusY,
        noiseCoefficient = advancedMaterial.noiseCoefficient,
        colors = colors,
        contentBlendMode = advancedMaterial.blurContentBlendMode,
        enabled = advancedMaterial.textureBlurEnabled,
    )
} else {
    textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = advancedMaterial.resolvedBlurRadius,
        noiseCoefficient = advancedMaterial.noiseCoefficient,
        colors = colors,
        contentBlendMode = advancedMaterial.blurContentBlendMode,
        enabled = advancedMaterial.textureBlurEnabled,
    )
}

val LocalTeakMaterialSpec = staticCompositionLocalOf { TeakMaterialSpec() }

val LocalTeakBackdrop = staticCompositionLocalOf<Backdrop?> { null }
