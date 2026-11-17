package com.aritxonly.bdfun

import com.aritxonly.teak.common.theme.TeakStyle

import com.aritxonly.bdfun.navigateToVideo
import androidx.compose.runtime.Composable
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.rememberTeakHaptics
import kotlinx.coroutines.launch

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainShellNavigation(settings: SettingsBindings, shell: MainShellState, dark: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ui = shell.ui
    val homeFeed = shell.feeds.home
    val tabPagerState = shell.navigation.pager
    val destinations = shell.navigation.destinations
    val tabItems = shell.navigation.items
    val appScope = shell.navigation.scope
    val selected = tabPagerState.currentPage
    val searchChrome = shell.chrome.search
    val wideNavigation = shell.backdrops.wideNavigation
    val haptics = rememberTeakHaptics()
    val onBarItem: (TeakFloatingTabItem) -> Unit = { item ->
        haptics.tap()
        val index = destinations
            .indexOfFirst { it.key == item.key }
            .coerceAtLeast(0)
        if (index == selected) {
            // 再次点击当前页 → 刷新
            when (index) {
                // 首页三段各有自己的刷新入口，别让直播/番剧去刷推荐。
                // 直播/番剧和推荐页一致：先平滑回顶，再拉刷新。
                0 -> when (ui.homeSection) {
                    1 -> AppGraph.livePageState.requestRefresh()
                    2 -> AppGraph.bangumiPageState.requestRefresh()
                    0 -> homeFeed.requestRefresh()
                }
                1 -> ui.dynamicRefreshTick++
            }
        } else {
            // 主 tab 不能左右滑，但点底栏切换仍保留横向滑动转场。
            appScope.launch { tabPagerState.animateScrollToPage(index) }
        }
    }
    MainBottomNavigation(
        settings, dark, wideNavigation, ui.showSearch, tabItems, destinations[selected].key,
        onBarItem = onBarItem,
        onSearch = {
            if (TeakStyle.fromStoredValue(settings.interfaceStyle).layout.hasBottomSearch) shell.bottom.open() else {
            appScope.launch { tabPagerState.scrollToPage(0) }
            searchChrome.reset()
            ui.showSearch = true
            ui.searchQuery = ""
            ui.searchSubmitted = null
            }
        },
        onBottomSearch = { shell.bottom.open() },
        onOpenVideo = { navigateToVideo(context, it) },
    )

}
