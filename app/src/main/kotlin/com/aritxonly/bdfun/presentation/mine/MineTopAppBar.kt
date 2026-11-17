package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.AdaptiveMiuixTopAppBar
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassRecipes
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MineTopAppBar(showSettings: Boolean, onSettings: () -> Unit) {
    val material = LocalTeakMaterialSpec.current
    val recipe = remember(material) { TeakGlassRecipes.floatingNavigation(material.fineTuning) }
    val haptics = rememberTeakHaptics()
    AdaptiveMiuixTopAppBar(title = "", largeTitle = "", color = Color.Transparent, actions = {
        // 只由采样层外的 MainMineChrome 调用，避免玻璃回读包含自身的主壳背景。
        if (showSettings) {
            if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
                // 经典样式：顶栏只保留图标，不加圆底。
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(40.dp)
                        .clickable(interactionSource = null, indication = null) {
                            haptics.tap(); onSettings()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MiuixIcons.Settings, "设置", tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                }
            } else {
                TeakGlassIconButton(onClick = { haptics.tap(); onSettings() }, size = 40.dp, recipe = recipe,
                    backdrop = LocalTeakBackdrop.current, materialAlpha = 1f) {
                    Icon(MiuixIcons.Settings, "设置", tint = MiuixTheme.colorScheme.onSurfaceContainer,
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    })
}
