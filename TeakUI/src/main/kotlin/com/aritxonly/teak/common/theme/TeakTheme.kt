package com.aritxonly.teak.common.theme

import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakMaterialFineTuning
import com.aritxonly.teak.common.material.TeakMaterialSpec
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 全局界面风格。
 *
 * - [Official]（默认）：现在的悬浮圆角 / 玻璃风格，完全保持原样。
 * - [Classic]：去掉悬浮感，栏目贴着屏幕边缘，用分割线区分；评论区也不再用白色圆角卡片。
 *
 * 用对象而不是 CompositionLocal，这样设置页改完后所有已组合的组件都会立即重新读取；
 * 只有显式判断 [isClassic] 的地方才会改变，没判断的地方仍然保持官方样式。
 */
object TeakUiStyle {
    var style by mutableStateOf(0)

    /** 0 = 官方，1 = 经典，2 = HyperOS 4（新底栏 + 搜索一镜到底）。 */
    val current: TeakStyle get() = TeakStyle.fromStoredValue(style)
    val design: TeakDesign get() = current.design
    val isClassic: Boolean get() = design == TeakDesign.Classic
    val isHyperOs4: Boolean get() = current == TeakStyle.HyperOs4
}

/**
 * TeakUI 的主题适配层。
 *
 * TeakUI 本身不绑定任何 App 的品牌色：宿主 App 传入 Miuix 的浅色/深色色板、
 * Material3 色板和字号倍率，这里统一注入 MiuixTheme、MaterialTheme、
 * LocalTeakMaterialSpec 和全局字号。
 */
@Composable
fun TeakTheme(
    dark: Boolean = isSystemInDarkTheme(),
    lightColors: Colors,
    darkColors: Colors,
    materialColors: ColorScheme,
    blurEnabled: Boolean = true,
    refractionEnabled: Boolean = true,
    fontScale: Float = 1f,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) {
        var owner = context
        while (owner is android.content.ContextWrapper && owner !is android.app.Activity) owner = owner.baseContext
        owner as? android.app.Activity
    }
    androidx.compose.runtime.DisposableEffect(activity, materialColors.background) {
        activity?.window?.let { window ->
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
            if (Build.VERSION.SDK_INT >= 30) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            }
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(materialColors.background.toArgb()))
        }
        onDispose { }
    }
    val mode = if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light
    val controller = remember(mode, lightColors, darkColors) {
        ThemeController(
            colorSchemeMode = mode,
            lightColors = lightColors,
            darkColors = darkColors,
        )
    }
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, fontScale) {
        Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * fontScale,
        )
    }
    val materialSpec = remember(blurEnabled, refractionEnabled) {
        TeakMaterialSpec(
            enabled = blurEnabled,
            blurEnabled = blurEnabled && Build.VERSION.SDK_INT >= 33,
            navigationTintAlpha = 0.72f,
            fineTuning = TeakMaterialFineTuning.Default.copy(
                refractionEnabled = refractionEnabled,
                postRefractionBlurEnabled = refractionEnabled,
            ),
        )
    }
    // 文字字重完全交给系统默认：不写死 350 / Light，用户系统里调粗调细会跟着变。
    val systemHaptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val haptics = if (hapticsEnabled) systemHaptics else SilentHapticFeedback
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalTeakContentColors provides teakContentColors(dark),
        LocalHapticsEnabled provides hapticsEnabled,
        androidx.compose.ui.platform.LocalHapticFeedback provides haptics,
    ) {
        MiuixTheme(controller = controller) {
            MaterialTheme(colorScheme = materialColors) {
                CompositionLocalProvider(LocalTeakMaterialSpec provides materialSpec) {
                    com.aritxonly.teak.common.modal.TeakModalHost { content() }
                }
            }
        }
    }
}
