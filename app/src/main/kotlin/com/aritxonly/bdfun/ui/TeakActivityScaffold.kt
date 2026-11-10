package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.hyperDarkColors
import com.aritxonly.bdfun.hyperLightColors
import com.aritxonly.teak.common.chrome.TeakPageChrome
import com.aritxonly.teak.common.chrome.TeakPageScaffold
import com.aritxonly.teak.common.chrome.TeakPageScrollState
import com.aritxonly.teak.common.theme.TeakTheme

/**
 * 独立 Activity 的统一外壳：主题 + 玻璃页面 chrome（顶栏 / 返回键 / backdrop）。
 *
 * 页面 chrome 本体已抽到 `TeakUI` 的 [TeakPageScaffold]，
 * 这里只负责把 App 的主题、色板和字号注入进去。
 *
 * [backRevealed] 传 null 表示跟随页面滚动；视频页传 false（纯箭头）。
 */
@Composable
fun TeakActivityScaffold(
    settings: SettingsStore,
    onBack: () -> Unit,
    backRevealed: Boolean? = null,
    showBackButton: Boolean = true,
    title: String? = null,
    topBarLeading: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    topBarCenter: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
    topBarActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    /** 采集层之外的覆盖层：这里的玻璃可以安全读取 backdrop（真模糊）。 */
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
    /** 顶栏之上的浮层，玻璃菜单用，避免被搜索按钮等顶栏按钮盖住。 */
    menuOverlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
    titleAlwaysVisible: Boolean = false,
    pageState: TeakPageScrollState? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.darkMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val lightColors = remember(settings.colorStyle) { hyperLightColors() }
    val darkColors = remember(settings.colorStyle) { hyperDarkColors() }
    val materialColors = if (dark) {
        darkColorScheme(
            primary = BrandPink,
            background = Color(0xFF0D0E11),
            surface = Color(0xFF121317),
            surfaceVariant = Color(0xFF1A1C21),
            onSurface = Color(0xFFF2F3F5),
            outline = Color(0xFF2B2E35),
        )
    } else {
        lightColorScheme(
            primary = BrandPink,
            background = Color(0xFFF3F3F3),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF7F7F7),
            onSurface = Color(0xFF16181D),
            outline = Color(0xFFE6E6E6),
        )
    }
    androidx.compose.runtime.SideEffect {
        com.aritxonly.teak.common.theme.TeakUiStyle.style = settings.interfaceStyle
    }
    TeakTheme(
        dark = dark,
        lightColors = lightColors,
        darkColors = darkColors,
        materialColors = materialColors,
        blurEnabled = settings.blurEnabled,
        refractionEnabled = settings.refractionEnabled,
        fontScale = settings.fontScale,
    ) {
        TeakPageScaffold(
            title = title,
            onBack = onBack,
            backRevealed = backRevealed,
            showBackButton = showBackButton,
            pageState = pageState ?: com.aritxonly.teak.common.chrome.rememberTeakPageScrollState(),
            backgroundColor = materialColors.background,
            contentModifier = Modifier
                .background(materialColors.background)
                .smartRefresh(settings.smartRefresh),
            topBarLeading = topBarLeading,
            topBarCenter = topBarCenter,
            topBarActions = topBarActions,
            overlay = overlay,
            menuOverlay = menuOverlay,
            titleAlwaysVisible = titleAlwaysVisible,
            content = content,
        )
    }
}
