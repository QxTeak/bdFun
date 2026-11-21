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
import com.aritxonly.teak.common.icons.rememberAntialiasedIconPainter
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
    val cinemaChrome = rememberTeakPageScrollState()
    val popularChrome = rememberTeakPageScrollState()
    val dynamicChrome = rememberTeakPageScrollState()
    val searchChrome = rememberTeakPageScrollState()
    val mineChrome = rememberTeakPageScrollState()
    val mineList = androidx.compose.foundation.lazy.rememberLazyListState()
    // 登录态放在进程级单例，LoginActivity 登录成功后直接写，这里立即刷新
    val session by AppGraph.session
    val ui = rememberMainShellUi(AppGraph.settings.homeChannels, AppGraph.settings.homeChannelOrder)
    // 搜索页渐变进度：和头像/消息那两个插槽的切换速率完全一致
    val searchReveal = rememberManualProgress(if (ui.showSearch) 1f else 0f, UiSwapDurationMs)

    // 再次点击当前底栏项 → 触发该页刷新
    val appScope = rememberCoroutineScope()
    val unreadMessageCount = rememberMainMessageBadge(AppGraph.session.identity)

    LaunchedEffect(Unit) {
        AppGraph.refreshSession()
    }
    // 视频页收起态点「回首页」：主壳可能还在后面的栈里，这里消费一次跨页请求。
    val mainTabRequests = AppGraph.mainTabRequests
    LaunchedEffect(mainTabRequests.pendingHome) {
        if (mainTabRequests.pendingHome) {
            tabPagerState.scrollToPage(0)
            ui.homeSection = 0
            mainTabRequests.consumeHome()
        }
    }
    // 首页数据放在 App 层：进视频/搜索/设置再返回时不会重新拉取，滚动位置也保留。
    val homeFeed = AppGraph.homeFeedState
    // 动态页每个分类独立状态；顶栏和滚动跟踪跟随当前分类。
    val dynamicFeed = rememberDynamicTabState(ui).feed
    // 首页三段各自注册回到顶部；动态页注册自己的列表。
    when (selected) {
        0 -> when (ui.homeSection) {
            1 -> com.aritxonly.bdfun.ui.RegisterScrollTop(AppGraph.livePageState.gridState, "live")
            2 -> com.aritxonly.bdfun.ui.RegisterScrollTop(AppGraph.popularPageState.gridState, "popular")
            3 -> com.aritxonly.bdfun.ui.RegisterScrollTop(AppGraph.bangumiPageState.gridState, "pgc")
            4 -> com.aritxonly.bdfun.ui.RegisterScrollTop(AppGraph.cinemaPageState.gridState, "cinema")
            0 -> com.aritxonly.bdfun.ui.RegisterScrollTop(homeFeed.gridState, "home")
        }
        1 -> com.aritxonly.bdfun.ui.RegisterScrollTop(dynamicFeed.listState, "dynamic")
    }
    TrackMainShellPages(homeFeed, dynamicFeed, mineList, homeChrome, liveChrome,
        bangumiChrome, cinemaChrome, popularChrome, dynamicChrome, mineChrome)
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
            selectedIcon = rememberAntialiasedIconPainter(rememberVectorPainter(destination.icon)),
            unselectedIcon = rememberAntialiasedIconPainter(rememberVectorPainter(destination.icon)),
            iconScale = if (destination.key == "home") 0.95f else 1f,
        )
    }

    val wideNavigation = isWideFoldTopBar() && LocalConfiguration.current.screenWidthDp >= 600
    val bottom = remember { BottomSearchShellStateHolder(ui, appScope, { searchChrome.reset() }) { AppGraph.settings.animationsEnabled } }
    return MainShellState(
        ui, MainShellNavigation(tabPagerState, destinations, tabItems, appScope),
        MainShellChrome(homeChrome, liveChrome, bangumiChrome, cinemaChrome, popularChrome, dynamicChrome, searchChrome, mineChrome, mineList),
        MainShellFeeds(session, homeFeed, dynamicFeed, unreadMessageCount),
        MainShellBackdrops(backdrop, searchBackdrop, wideNavigation), searchReveal,
        bottom, { AppGraph.searchStateHolder() },
    )
}

@Composable
private fun rememberDynamicTabState(ui: MainShellUiStateHolder): DynamicTabState {
    val tabs = com.aritxonly.bdfun.core.domain.DynamicTabPolicy.ordered(
        AppGraph.settings.dynamicFeedTabs, AppGraph.settings.dynamicFeedTabOrder,
    )
    LaunchedEffect(tabs) {
        if (tabs.isNotEmpty() && tabs.none { it.key == ui.dynamicTab }) {
            ui.dynamicTab = tabs.first().key
        }
    }
    return DynamicTabState(tabs, AppGraph.dynamicFeedStateFor(ui.dynamicTab))
}

internal class DynamicTabState(
    val tabs: List<com.aritxonly.bdfun.core.domain.DynamicFeedTab>,
    val feed: com.aritxonly.bdfun.feature.feed.presentation.FeedState,
)

/** 首页各段 / 动态 / 我的各自绑定滚动进度；拆出来让主函数保持在尺寸门槛内。 */
@Composable
private fun TrackMainShellPages(
    homeFeed: com.aritxonly.bdfun.feature.home.presentation.HomeFeedState,
    dynamicFeed: com.aritxonly.bdfun.feature.feed.presentation.FeedState,
    mineList: androidx.compose.foundation.lazy.LazyListState,
    home: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    live: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    bangumi: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    cinema: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    popular: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    dynamic: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    mine: com.aritxonly.teak.common.chrome.TeakPageScrollState,
) {
    TrackTeakPageScroll(homeFeed.gridState, home)
    TrackTeakPageScroll(dynamicFeed.listState, dynamic)
    TrackTeakPageScroll(mineList, mine)
    TrackTeakPageScroll(AppGraph.livePageState.gridState, live)
    TrackTeakPageScroll(AppGraph.popularPageState.gridState, popular)
    TrackTeakPageScroll(AppGraph.bangumiPageState.gridState, bangumi)
    TrackTeakPageScroll(AppGraph.cinemaPageState.gridState, cinema)
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

@Composable
private fun rememberMainShellUi(channels: Set<String>, order: List<String>): MainShellUiStateHolder {
    val ui = remember { MainShellUiStateHolder() }
    LaunchedEffect(channels, order) {
        ui.homeSection = com.aritxonly.bdfun.core.domain.HomeChannelPolicy.selected(ui.homeSection, channels, order)
    }
    return ui
}
