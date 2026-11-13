package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.effect.bg.BgEffectBackground
import top.yukonga.miuix.kmp.effect.bg.DeviceType
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OnboardingBackground(
    playing: Boolean,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val shader = remember { isRuntimeShaderSupported() }
    BgEffectBackground(
        dynamicBackground = playing,
        deviceType = if (LocalConfiguration.current.screenWidthDp >= 600) {
            DeviceType.PAD
        } else {
            DeviceType.PHONE
        },
        isDarkTheme = surface.luminance() < 0.5f,
        surface = surface,
        modifier = modifier.fillMaxSize(),
        bgModifier = if (shader) Modifier.layerBackdrop(backdrop) else Modifier,
        isFullSize = true,
        content = content,
    )
}
