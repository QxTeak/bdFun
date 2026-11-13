package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.interaction.blockTouches


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec


val HomeTopBarHeight: Dp = 64.dp

@Composable
internal fun HomeTopBar(content: HomeTopBarContent, search: HomeTopBarSearch, actions: HomeTopBarActions,
    tabs: HomeTopBarTabs? = null,
    modifier: Modifier = Modifier) {
    val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val density = androidx.compose.ui.platform.LocalDensity.current
    val wide = with(density) { windowSize.width.toDp() >= 600.dp } &&
        com.aritxonly.teak.common.chrome.isWideFoldTopBar()
    val searchWidth = with(density) { (windowSize.width.toDp() * 0.45f).coerceIn(280.dp, 480.dp) }
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val backdrop = LocalTeakBackdrop.current
    val searchInteraction = remember { MutableInteractionSource() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    val material = remember(wide, searchWidth, recipe, backdrop, searchInteraction, classic) {
        HomeTopBarMaterial(wide, searchWidth, recipe, backdrop, searchInteraction, classic)
    }
    Box(modifier = modifier.fillMaxWidth().height(HomeTopBarHeight + statusBarTop)) {
        HomeTopBarBackground(classic)
        if (search.active) {
            Box(modifier = Modifier.matchParentSize().blockTouches(true))
        }
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp).height(HomeTopBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeTopBarLeft(content, search, actions)
            Spacer(Modifier.width(10.dp))
            if (tabs != null) {
                // HyperOS 4：首页不再有顶部搜索框，中间位置让给和动态页同款的分段控件。
                // 用居中容器而不是 weight，避免左右图标宽度变化时 tab 跟着抖。
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    TeakSegmentedBar(
                        options = tabs.options,
                        selected = tabs.selected.coerceIn(0, tabs.options.lastIndex),
                        itemWidth = 64.dp,
                        materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                        onSelect = tabs.onSelect,
                    )
                }
            } else {
                HomeSearchSurface(search, actions, material)
            }
            if (wide) Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            HomeTopBarRight(content, search, actions, material)
        }
    }
}
