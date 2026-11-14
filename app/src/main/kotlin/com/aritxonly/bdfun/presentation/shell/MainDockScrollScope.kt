package com.aritxonly.bdfun

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.teak.common.chrome.isWideFoldTopBar
import com.aritxonly.teak.common.navigation.LocalTeakDockScrollFraction
import com.aritxonly.teak.common.theme.TeakUiStyle

@Composable
internal fun MainDockScrollScope(
    settings: SettingsBindings, shell: MainShellState, content: @Composable (Modifier) -> Unit,
) {
    // 只收起宽折叠竖屏；横屏及普通窄竖屏始终保留完整标签和展开高度。
    val window = LocalWindowInfo.current.containerSize
    val enabled = window.height > window.width && isWideFoldTopBar() && !TeakUiStyle.isClassic &&
        !shell.ui.showSearch && !shell.bottom.showBottomSearch
    val distance = with(LocalDensity.current) { 6.dp.toPx() }
    val state = remember(shell.navigation.pager.currentPage, enabled, distance, settings.interfaceStyle) {
        MainDockScrollStateHolder(distance)
    }
    val fraction by animateFloatAsState(
        if (enabled && state.collapsed) 1f else 0f,
        spring(dampingRatio = 1f, stiffness = 350f), label = "scrolling-dock",
    )
    CompositionLocalProvider(LocalTeakDockScrollFraction provides fraction) {
        content(if (enabled) Modifier.nestedScroll(state) else Modifier)
    }
}
