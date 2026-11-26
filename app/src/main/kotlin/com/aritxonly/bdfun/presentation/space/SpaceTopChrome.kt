package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.findHostActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakTopGlassButton
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal class SpaceChromeState {
    val tools = SpaceToolsUiState()
    /** 省略号形变菜单；和收藏页共用同一套组件。 */
    val morphMenu = com.aritxonly.teak.common.navigation.TeakMorphMenuState()
    var wide by mutableStateOf(false)
    var title by mutableStateOf("")
    var profileWidth by mutableStateOf(320.dp)
    var headerProgress by mutableFloatStateOf(0f)
    var contentProgress by mutableFloatStateOf(0f)
    var bannerIsDark by mutableStateOf(true)
    var tabY by mutableFloatStateOf(Float.POSITIVE_INFINITY)
    var visible by mutableStateOf(false)
    var selected by mutableIntStateOf(0)
    var position: androidx.compose.runtime.State<Float>? by mutableStateOf(null)
    var onSelect: (Int) -> Unit = {}
}

@Composable
internal fun SpaceTopChrome(state: SpaceChromeState, onBack: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val pageGlyph = colors.onBackground
    val bannerGlyph = if (state.bannerIsDark) Color.White else Color.Black
    val glyph by androidx.compose.animation.animateColorAsState(
        targetValue = androidx.compose.ui.graphics.lerp(bannerGlyph, pageGlyph, state.headerProgress),
        animationSpec = androidx.compose.animation.core.tween(240), label = "space-icon-contrast",
    )
    val context = LocalContext.current
    androidx.compose.runtime.SideEffect {
        context.findHostActivity()?.let {
            val lightIcons = (if (state.wide) pageGlyph else glyph).luminance() >= 0.5f
            if (it is SpaceActivity) it.syncSpaceStatusIcons(lightIcons)
            else androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
                .isAppearanceLightStatusBars = !lightIcons
        }
    }
    val actions: @Composable androidx.compose.foundation.layout.RowScope.(Color) -> Unit = { tint ->
        TeakTopGlassButton(onClick = { state.tools.search = true }) {
            Icon(MiuixIcons.Search, contentDescription = "搜索", tint = tint, modifier = Modifier.size(20.dp))
        }
        com.aritxonly.teak.common.navigation.TeakMorphMenuAnchor(state.morphMenu) { onClick ->
            TeakTopGlassButton(onClick = onClick) {
                Icon(MiuixIcons.More, contentDescription = "更多", tint = tint, modifier = Modifier.size(20.dp))
            }
        }
    }
    val tabs: @Composable (Dp) -> Unit = { itemWidth ->
        TeakPagerTabs(options = listOf("主页", "动态", "投稿", "小店"), selected = state.selected,
            onSelect = { state.onSelect(it) }, position = state.position, itemWidth = itemWidth,
            materialProgress = if (state.wide) 1f
                else com.aritxonly.teak.common.chrome.rememberManualProgress(state.headerProgress, 280f))
    }
    if (state.wide) SpaceWideTopChrome(state, onBack, glyph, pageGlyph, tabs, actions)
    else SpaceCompactTopChrome(state, onBack, glyph, actions)

}
