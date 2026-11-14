package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import androidx.compose.runtime.Composable
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
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
    val onBarItem: (TeakFloatingTabItem) -> Unit = { item ->
        val index = destinations
            .indexOfFirst { it.key == item.key }
            .coerceAtLeast(0)
        if (index == selected) {
            // 再次点击当前页 → 刷新
            when (index) {
                // 首页三段各有自己的刷新入口，别让直播/番剧去刷推荐。
                0 -> when (ui.homeSection) {
                    1 -> appScope.launch { AppGraph.livePageState.refresh() }
                    2 -> appScope.launch { AppGraph.bangumiPageState.refresh() }
                    else -> homeFeed.requestRefresh()
                }
                1 -> ui.dynamicRefreshTick++
            }
        } else {
            appScope.launch { tabPagerState.animateScrollToPage(index) }
        }
    }
    MainBottomNavigation(
        settings, dark, wideNavigation, ui.showSearch, tabItems, destinations[selected].key,
        onBarItem = onBarItem,
        onSearch = {
            if (settings.interfaceStyle == 2) shell.bottom.open() else {
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
