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
        // 切页时先收掉长按形变菜单：它挂在主壳采样树外，不会随首页一起离开，
        // 留着会停在新页面上，锚点也还是旧卡片的位置。
        homeFeed.dismissMenu()
        AppGraph.dynamicFeedStateFor(ui.dynamicTab).dismissMenu()
        val index = destinations
            .indexOfFirst { it.key == item.key }
            .coerceAtLeast(0)
        if (index == tabPagerState.currentPage) {
            val chrome = when (index) {
                0 -> when (ui.homeSection) {
                    1 -> shell.chrome.live
                    2 -> shell.chrome.popular
                    3 -> shell.chrome.bangumi
                    4 -> shell.chrome.cinema
                    else -> shell.chrome.home
                }
                1 -> shell.chrome.dynamic
                else -> shell.chrome.mine
            }
            val scroll = chrome.scrollableState
            val refresh = index in 0..1 && com.aritxonly.bdfun.core.domain.TabClickPolicy.shouldRefresh(
                settings.tabClickRefresh, scroll != null && !scroll.canScrollBackward,
            )
            if (!refresh) {
                appScope.launch { scrollMainPageToTop(scroll) }
            } else {
                when (index) {
                    // 首页三段各有自己的刷新入口，别让直播/番剧去刷推荐。
                    // 直播/番剧和推荐页一致：先平滑回顶，再拉刷新。
                    0 -> when (ui.homeSection) {
                        1 -> AppGraph.livePageState.requestRefresh()
                        2 -> AppGraph.popularPageState.requestRefresh()
                        3 -> AppGraph.bangumiPageState.requestRefresh()
                        4 -> AppGraph.cinemaPageState.requestRefresh()
                        0 -> homeFeed.requestRefresh()
                    }
                    1 -> {
                        val dynamic = shell.feeds.dynamic
                        if (dynamic.filterMid != null) {
                            // 特定 UP 页里的「再次点动态」：留在本页静默刷新。
                            // 不能走 refreshTick（那条路会拉下拉指示器，弹出小电视）。
                            appScope.launch { dynamic.refresh(shell.feeds.session != null) }
                        } else {
                            ui.dynamicRefreshTick++
                        }
                    }
                }
            }
        } else {
            // 主 tab 不能左右滑；点底栏切换仍保留横向滑动，但用先快后慢的缓动。
            appScope.launch {
                tabPagerState.animateScrollToPage(
                    index,
                    animationSpec = com.aritxonly.teak.common.navigation.TeakPageSwitchSpec(),
                )
            }
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
