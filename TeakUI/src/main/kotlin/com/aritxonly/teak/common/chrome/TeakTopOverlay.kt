package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassRecipes

/** 顶部玻璃按钮共享的“是否已露出玻璃底”状态。 */
internal val LocalTeakTopOverlayRevealed = compositionLocalOf { true }

/**
 * 页面顶部 overlay 的统一布局。
 *
 * 左边默认是返回键，也可以放别的引导内容；右边放操作按钮；中间可放标题/筛选。
 * 所有内容都在 backdrop 采集层之外，玻璃按钮可以安全采样页面背景。
 */
@Composable
fun TeakTopOverlay(
    revealed: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    light: Boolean = false,
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
    center: @Composable BoxScope.() -> Unit = {},
) {
    CompositionLocalProvider(LocalTeakTopOverlayRevealed provides revealed) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(TeakChromeDefaults.TopOverlayHeight)
                .padding(
                    horizontal = TeakChromeDefaults.TopOverlayHorizontalPadding,
                    vertical = TeakChromeDefaults.TopOverlayVerticalPadding,
                ),
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(TeakChromeDefaults.TopOverlayItemSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    TeakGlassBackButton(
                        onClick = onBack,
                        revealed = revealed,
                        light = light,
                    )
                }
                leading()
            }
            Box(modifier = Modifier.align(Alignment.Center)) {
                center()
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(TeakChromeDefaults.TopOverlayItemSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                trailing()
            }
        }
    }
}

/**
 * 顶部通用玻璃圆钮：与返回键同一套 300ms 渐显。
 * 必须放在 [TeakTopOverlay] 的 leading/trailing 里使用。
 */
@Composable
fun TeakTopGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val revealed = LocalTeakTopOverlayRevealed.current
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = remember(advancedMaterial) {
        TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    }
    val fallbackReveal = rememberManualProgress(if (revealed) 1f else 0f, 300f)
    val reveal = if (LocalProgressiveTopBarMaterialHosted.current) LocalTopBarButtonMaterialProgress.current else fallbackReveal

    Box(modifier = modifier.size(40.dp)) {
        if (reveal > 0.01f) {
            val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
            if (classic) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = reveal }
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainerHigh,
                        )
                        .clickable(interactionSource = null, indication = null, onClick = onClick),
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = reveal },
                ) {
                    TeakGlassIconButton(
                        onClick = onClick,
                        size = 40.dp,
                        recipe = recipe,
                        backdrop = LocalTeakBackdrop.current,
                    ) {}
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(interactionSource = null, indication = null, onClick = onClick),
            )
        }
        Box(
            modifier = Modifier.align(Alignment.Center),
            content = content,
        )
    }
}
