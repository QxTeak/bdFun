package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.isWideFoldTopBar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.ui.ScrollTopButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import com.aritxonly.teak.common.search.teakSearchUnderlay
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
internal fun MainScaffold(settings: SettingsBindings, shell: MainShellState, dark: Boolean, background: Color) {
    MainDockScrollScope(settings, shell) { modifier ->
        MainScaffoldContent(settings, shell, dark, background, modifier)
    }
}

@Composable
private fun MainScaffoldContent(settings: SettingsBindings, shell: MainShellState, dark: Boolean, background: Color, modifier: Modifier) {
    val ui = shell.ui
    val homeFeed = shell.feeds.home
    val tabPagerState = shell.navigation.pager
    val backdrop = shell.backdrops.main
    val wideNavigation = shell.backdrops.wideNavigation
    val floatingBottomSpacing = if (isWideFoldTopBar()) 0.dp else 4.dp
    val pageFloat = (tabPagerState.currentPage + tabPagerState.currentPageOffsetFraction)
        .coerceAtLeast(0f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        val underlay = Modifier.teakSearchUnderlay(
            shell.bottom.bottomSearchProgress.value, shell.bottom.bottomUnderlayAlpha.value,
            shell.bottom.bottomBlurProgress.value,
        )
        Box(Modifier.fillMaxSize()) {
            // 录制正文已经上移和模糊后的画面，顶栏和屏幕正文使用同一份变换。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .then(underlay),
            ) {
                // 三个 tab：横向相连的 pager（可拖拽，切换是同一套非线性动画）
                // 外面包一层更大的触摸阈值，手指轻抖不会误翻页。
                // 分段只在 HyperOS 4 风格下可见；切回官方/经典时强制回到推荐，
                // 否则会停在一个没有入口可以离开的空页面上。
                MainTabPager(tabPagerState, homeFeed, shell.feeds.session, settings, ui.dynamicRefreshTick,
                    shell.chrome.mineList,
                    // 三种界面风格都保留首页的推荐/直播/番剧分段。
                    homeSection = ui.homeSection,
                    homeInteractive = !ui.showSearch,
                    onHomeSectionChange = { ui.homeSection = it },
                    onHomeSectionPositionChange = { ui.homeSectionPosition.floatValue = it },
                    dynamic = DynamicTabPagerBindings(
                        tab = ui.dynamicTab,
                        clickTick = ui.dynamicTabClickTick,
                        onTabChange = { ui.dynamicTab = it },
                        onPositionChange = { ui.dynamicTabPosition.floatValue = it },
                    ),
                    onDynamicUpProgress = { ui.dynamicUpProgress.floatValue = it },
                )

            }

            Box(Modifier.fillMaxSize().then(underlay)) {
                ScrollTopButton(
                    settings,
                    bottomObstruction = if (ui.showSearch || shell.bottom.showBottomSearch) 0.dp else
                    TeakFloatingTabBarDefaults.CurrentHeight + when {
                        wideNavigation -> MainWideNavigationEdgePadding
                        settings.bottomBarRaise -> floatingBottomSpacing + TeakChromeDefaults.RaisedBottomBarExtra
                        else -> floatingBottomSpacing
                    },
                )

                MainSearchLayer(settings, shell)
            }
        }
        // 点顶栏回顶：常开。画在顶栏之前（下面），搜索框 / 分段 / 图标照常优先拿到点击。
        MainStatusBarScrollToTop(shell)
        // 两个顶栏都挂在采集层之外：底栏搜索的一镜到底会把内容整体上移并模糊，
        // 顶栏只跟着缩放和淡出，不应该再跟着上下位移一次，否则会双重运动。
        MainHomeChrome(settings, shell, pageFloat)
        MainMineChrome(shell, pageFloat)
        MainFeedChrome(shell, pageFloat)

        Box(Modifier.fillMaxSize().graphicsLayer {
                val progress = shell.bottom.bottomTabBarProgress.value
                translationY = 96.dp.toPx() * progress
                scaleX = 1f - 0.4f * progress
                scaleY = scaleX
                alpha = 1f - progress
        }) { MainShellNavigation(settings, shell, dark) }
        MainBottomSearchLayer(settings, shell)
    }
}

/**
 * 点顶栏回顶：取当前可见页自己的滚动容器（搜索页用搜索列表的状态）。
 * 只铺状态栏 + 顶栏高度，并且画在顶栏按钮之前，点击优先级不变。
 */
@Composable
private fun MainStatusBarScrollToTop(shell: MainShellState) {
    val ui = shell.ui
    val page = shell.navigation.pager.currentPage
    // 动态页进入 UP 页后，原分类顶栏已经让位，肩膀以下就是头像行：
    // 回顶的点击区只能留在状态栏里，不然会把头像的点击吃掉。
    val upPage = page == 1 && AppGraph.dynamicFeedStateFor(ui.dynamicTab).filterMid != null
    com.aritxonly.teak.common.chrome.TeakStatusBarScrollToTop(
        pageState = when {
            ui.showSearch -> shell.chrome.search
            page == 1 -> shell.chrome.dynamic
            page == 2 -> shell.chrome.mine
            ui.homeSection == 2 -> shell.chrome.popular
            ui.homeSection == 3 -> shell.chrome.bangumi
            else -> shell.chrome.home
        },
        belowStatusBar = if (upPage) 0.dp else TeakChromeDefaults.TopBarHeight,
    )
}
