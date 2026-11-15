package com.aritxonly.bdfun

import androidx.lifecycle.repeatOnLifecycle
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.isWideFoldTopBar
import com.aritxonly.teak.common.chrome.rememberTeakPageScrollState

import androidx.compose.foundation.background
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.size
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem
import com.aritxonly.bdfun.ui.UiSwapDurationMs
import com.aritxonly.bdfun.ui.rememberManualProgress
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@Composable
internal fun rememberMainShellState(background: Color): MainShellState {
    val destinations = rememberMainDestinations()
    // 三个 tab 用 HorizontalPager：页面之间横向相连，手拖和切换都是同一套非线性动画
    val tabPagerState = rememberPagerState(pageCount = { destinations.size })
    val selected = tabPagerState.currentPage
    val homeChrome = rememberTeakPageScrollState()
    val liveChrome = rememberTeakPageScrollState()
    val bangumiChrome = rememberTeakPageScrollState()
    val dynamicChrome = rememberTeakPageScrollState()
    val searchChrome = rememberTeakPageScrollState()
    val mineChrome = rememberTeakPageScrollState()
    val mineList = androidx.compose.foundation.lazy.rememberLazyListState()
    // 登录态放在进程级单例，LoginActivity 登录成功后直接写，这里立即刷新
    val session by AppGraph.session
    val ui = remember { MainShellUiStateHolder() }
    // 搜索页渐变进度：和头像/消息那两个插槽的切换速率完全一致
    val searchReveal = rememberManualProgress(if (ui.showSearch) 1f else 0f, UiSwapDurationMs)

    // 再次点击当前底栏项 → 触发该页刷新
    val appScope = rememberCoroutineScope()
    val unreadMessageCount = rememberMainMessageBadge(AppGraph.session.identity)

    LaunchedEffect(Unit) {
        AppGraph.refreshSession()
    }
    // 首页数据放在 App 层：进视频/搜索/设置再返回时不会重新拉取，滚动位置也保留。
    val homeFeed = AppGraph.homeFeedState
    val dynamicFeed = AppGraph.dynamicFeedState
    // 首页 / 动态页各自注册回到顶部；其他底栏页不显示。
    when (selected) {
        0 -> com.aritxonly.bdfun.ui.RegisterScrollTop(homeFeed.gridState, "home")
        1 -> com.aritxonly.bdfun.ui.RegisterScrollTop(dynamicFeed.listState, "dynamic")
    }
    TrackTeakPageScroll(homeFeed.gridState, homeChrome)
    TrackTeakPageScroll(dynamicFeed.listState, dynamicChrome)
    TrackTeakPageScroll(mineList, mineChrome)
    // 直播 / 番剧与推荐页同一套：滚动进度驱动顶栏渐进模糊和下滑玻璃。
    TrackTeakPageScroll(AppGraph.livePageState.gridState, liveChrome)
    TrackTeakPageScroll(AppGraph.bangumiPageState.gridState, bangumiChrome)
    // 参考 HyperIsland 的做法：整屏一块 backdrop，录制时先铺一层 surface 再画内容
    val backdrop = rememberLayerBackdrop { drawRect(background); drawContent() }
    // 搜索页自己一块：首页顶栏的渐变模糊和筛选菜单都采样它，复用同一套页面结构。
    val searchBackdrop = rememberLayerBackdrop {
        drawRect(background)
        drawContent()
    }
    val tabItems = destinations.map { destination ->
        TeakFloatingTabItem(
            key = destination.key,
            label = destination.label,
            selectedIcon = rememberVectorPainter(destination.icon),
            unselectedIcon = rememberVectorPainter(destination.icon),
            iconScale = if (destination.key == "home") 0.95f else 1f,
        )
    }

    val wideNavigation = isWideFoldTopBar() && LocalConfiguration.current.screenWidthDp >= 600
    val bottom = remember { BottomSearchShellStateHolder(ui, appScope, { searchChrome.reset() }) { AppGraph.settings.animationsEnabled } }
    return MainShellState(
        ui, MainShellNavigation(tabPagerState, destinations, tabItems, appScope),
        MainShellChrome(homeChrome, liveChrome, bangumiChrome, dynamicChrome, searchChrome, mineChrome, mineList),
        MainShellFeeds(session, homeFeed, dynamicFeed, unreadMessageCount),
        MainShellBackdrops(backdrop, searchBackdrop, wideNavigation), searchReveal,
        bottom, { AppGraph.searchStateHolder() },
    )
}

@Composable
private fun rememberMainMessageBadge(identity: com.aritxonly.bdfun.core.application.SessionIdentity): Int {
    val owner = identity.mid
    val messageBadge = remember(identity) { AppGraph.messageBadgeStateHolder(owner) }
    val messageBadgeState by messageBadge.state.collectAsState()
    val unreadMessageCount = messageBadgeState.unreadCount
    val unreadLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(identity, unreadLifecycleOwner) {
        if (owner == null) return@LaunchedEffect
        unreadLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                messageBadge.refresh()
                kotlinx.coroutines.delay(60_000)
            }
        }
    }
    return unreadMessageCount
}
