package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.interaction.blockTouches


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec


val HomeTopBarHeight: Dp = 64.dp
/** 官方风格下，推荐/直播/番剧分段条单独占的一行。 */
val HomeTopBarTabsHeight: Dp = 40.dp
val HomeTopBarHeightWithTabs: Dp = HomeTopBarHeight + HomeTopBarTabsHeight

/** 各页统一的顶栏高度：官方与经典多一行分段条，其余风格保持原高度。 */
@Composable
internal fun officialHomeBarSingleRow(interfaceStyle: Int): Boolean {
    val window = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    return interfaceStyle == 0 && window.width > window.height
}

@Composable
fun homeTopBarHeight(interfaceStyle: Int): Dp =
    if ((interfaceStyle == 0 && !officialHomeBarSingleRow(interfaceStyle)) || interfaceStyle == 1) {
        HomeTopBarHeightWithTabs
    } else HomeTopBarHeight

@Composable
internal fun HomeTopBar(content: HomeTopBarContent, search: HomeTopBarSearch, actions: HomeTopBarActions,
    tabs: HomeTopBarTabs? = null,
    tabsCollapseProgress: Float = 0f,
    resultActions: (@Composable () -> Unit)? = null,
    singleRowSearchTabs: Boolean = false,
    modifier: Modifier = Modifier) {
    val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val density = androidx.compose.ui.platform.LocalDensity.current
    val wide = with(density) { windowSize.width.toDp() >= 600.dp }
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
    if (resultActions != null) {
        HomeTopBarSearchRow(content, search, actions, material, resultActions)
        return
    }
    // 搜索展开时让出高度：结果页从顶栏下沿开始，不会被分段条盖住。
    val visibleTabs = tabs.takeUnless { search.active }
    val tabsBelowSearch = visibleTabs?.belowSearch == true && !singleRowSearchTabs
    // 分段条高度固定，收起只做淡出+轻微上移：高度收缩 + clipToBounds 会在顶栏底部留下一条硬切线。
    val tabsAway = tabsCollapseProgress.coerceIn(0f, 1f)
    val totalHeight = HomeTopBarHeight + if (tabsBelowSearch) HomeTopBarTabsHeight else 0.dp
    Box(modifier = modifier.fillMaxWidth().height(totalHeight + statusBarTop)) {
        HomeTopBarBackground(classic)
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            val inline = visibleTabs
            val inlineTabs = inline != null && !tabsBelowSearch
            if (search.active) {
                HomeTopBarSearchRow(content, search, actions, material)
            } else if (singleRowSearchTabs) {
                HomeTopBarLandscapeRow(content, search, actions, visibleTabs, material)
            } else if (wide && inlineTabs && inline != null) {
                HomeTopBarWideTabsRow(content, search, actions, inline, material)
            } else {
                HomeTopBarMainRow(content, search, actions, visibleTabs, material, wide, tabsBelowSearch)
            }
            if (visibleTabs != null && tabsBelowSearch) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(HomeTopBarTabsHeight)
                        .graphicsLayer {
                            translationY = -12.dp.toPx() * tabsAway
                            alpha = 1f - tabsAway
                        },
                ) { HomeTopBarTabsBelow(visibleTabs) }
            }
        }
    }
}

/** 横屏：分段条相对整行真正居中，不再被左右图标宽度带偏。 */
@Composable
private fun HomeTopBarWideTabsRow(
    content: HomeTopBarContent, search: HomeTopBarSearch, actions: HomeTopBarActions,
    tabs: HomeTopBarTabs, material: HomeTopBarMaterial,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(HomeTopBarHeight)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(Modifier.align(Alignment.CenterStart)) {
            HomeTopBarLeft(content, search, actions)
        }
        TeakSegmentedBar(
            options = tabs.options,
            scrollable = androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp < 600,
            maxVisibleItems = 3,
            selected = tabs.selected.coerceIn(0, tabs.options.lastIndex),
            itemWidth = 64.dp,
            materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
            onSelect = tabs.onSelect,
            position = tabs.position,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(Modifier.align(Alignment.CenterEnd)) {
            HomeTopBarRight(content, search, actions, material)
        }
    }
}

@Composable
private fun HomeTopBarMainRow(
    content: HomeTopBarContent, search: HomeTopBarSearch, actions: HomeTopBarActions,
    tabs: HomeTopBarTabs?, material: HomeTopBarMaterial, wide: Boolean, tabsBelowSearch: Boolean,
) {
    Row(
        // 先固定整行 64dp，再在内部留 6dp 上下边距；否则 padding+height 会把行撑到 76dp，
        // 第二行分段条被顶下去、底部还会被外层高度裁掉（看起来比 HyperOS 4 小一圈）。
        modifier = Modifier.fillMaxWidth()
            .height(HomeTopBarHeight)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeTopBarLeft(content, search, actions)
        Spacer(Modifier.width(10.dp))
        if (tabs != null && !tabsBelowSearch) {
            // HyperOS 4：首页不再有顶部搜索框，中间位置让给和动态页同款的分段控件。
            // 用居中容器而不是 weight，避免左右图标宽度变化时 tab 跟着抖。
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TeakSegmentedBar(
                    options = tabs.options,
                    scrollable = androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp < 600,
                    maxVisibleItems = 3,
                    selected = tabs.selected.coerceIn(0, tabs.options.lastIndex),
                    itemWidth = 64.dp,
                    materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                    onSelect = tabs.onSelect,
                    position = tabs.position,
                )
            }
        } else if (wide) {
            // 横屏搜索页：搜索框相对整行居中，不再贴左/靠右。
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HomeSearchSurface(search, actions, material)
                }
            }
        } else {
            HomeSearchSurface(search, actions, material)
        }
        Spacer(Modifier.width(10.dp))
        HomeTopBarRight(content, search, actions, material)
    }
}

/** 官方风格：搜索框保持原位，分段条放在它下面一行。 */
@Composable
private fun HomeTopBarTabsBelow(tabs: HomeTopBarTabs) {
    Box(
        modifier = Modifier.fillMaxWidth().height(HomeTopBarTabsHeight),
        contentAlignment = Alignment.Center,
    ) {
        TeakSegmentedBar(
            options = tabs.options,
            scrollable = androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp < 600,
            maxVisibleItems = 3,
            selected = tabs.selected.coerceIn(0, tabs.options.lastIndex),
            itemWidth = 64.dp,
            materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
            onSelect = tabs.onSelect,
            position = tabs.position,
        )
    }
}

/** 横屏官方布局保留左侧搜索与右侧频道，各自使用受约束的空间。 */
@Composable
private fun HomeTopBarLandscapeRow(
    content: HomeTopBarContent, search: HomeTopBarSearch, actions: HomeTopBarActions,
    tabs: HomeTopBarTabs?, material: HomeTopBarMaterial,
) {
    Row(
        Modifier.fillMaxWidth().height(HomeTopBarHeight)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeTopBarLeft(content, search, actions)
        Spacer(Modifier.width(10.dp))
        // 此处按剩余宽度分配，避免固定的宽屏搜索宽度挤出频道和消息按钮。
        val flexibleMaterial = remember(material) {
            HomeTopBarMaterial(false, material.searchWidth, material.recipe, material.backdrop,
                material.searchInteraction, material.classic)
        }
        HomeSearchSurface(search, actions, flexibleMaterial)
        if (tabs != null) {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                TeakSegmentedBar(
                    options = tabs.options,
                    selected = tabs.selected.coerceIn(0, tabs.options.lastIndex),
                    onSelect = tabs.onSelect,
                    itemWidth = 64.dp,
                    materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                    position = tabs.position,
                    scrollable = true,
                    maxVisibleItems = 3,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        HomeTopBarRight(content, search, actions, material)
    }
}

@Composable
private fun HomeTopBarSearchRow(
    content: HomeTopBarContent, search: HomeTopBarSearch,
    actions: HomeTopBarActions, material: HomeTopBarMaterial,
    resultActions: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().height(HomeTopBarHeight).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        HomeTopBarLeft(content, search, actions)
        Spacer(Modifier.width(10.dp))
        HomeSearchSurface(search, actions, material)
        if (resultActions != null) {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) { resultActions() }
        }
    }
}
