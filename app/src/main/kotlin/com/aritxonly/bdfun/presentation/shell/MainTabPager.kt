package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToWatchLater
import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToSpace
import com.aritxonly.bdfun.navigateToSettings
import com.aritxonly.bdfun.navigateToMySent
import com.aritxonly.bdfun.navigateToLogin
import com.aritxonly.bdfun.navigateToLibrary
import com.aritxonly.bdfun.navigateToHistory
import com.aritxonly.bdfun.navigateToFeed
import com.aritxonly.bdfun.navigateToCache
import com.aritxonly.teak.common.navigation.TeakPageSwitchSpec
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import com.aritxonly.bdfun.ui.rememberStricterSwipeConfiguration
import com.aritxonly.bdfun.core.model.user.UserProfile
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.FeedScreen
import com.aritxonly.bdfun.ui.MineScreen
import com.aritxonly.bdfun.ui.homeTopBarHeight

@androidx.compose.runtime.Stable
internal data class DynamicTabPagerBindings(
    val tab: String = "all",
    /** 顶部分段点击计数：点当前项也要能退出 UP 模式。 */
    val clickTick: Int = 0,
    val onTabChange: (String) -> Unit = {},
    val onPositionChange: (Float) -> Unit = {},
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun MainTabPager(
    pager: androidx.compose.foundation.pager.PagerState,
    homeFeed: com.aritxonly.bdfun.feature.home.presentation.HomeFeedState,
    session: UserProfile?, settings: SettingsBindings, dynamicRefreshTick: Int,
    mineList: androidx.compose.foundation.lazy.LazyListState,
    homeSection: Int = 0,
    homeInteractive: Boolean = true,
    onHomeSectionChange: (Int) -> Unit = {},
    onHomeSectionPositionChange: (Float) -> Unit = {},
    dynamic: DynamicTabPagerBindings = DynamicTabPagerBindings(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val topBarHeight = homeTopBarHeight(settings.interfaceStyle)
    CompositionLocalProvider(
        LocalViewConfiguration provides rememberStricterSwipeConfiguration(),
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            // 主 tab（首页/动态/我的）只由底栏点击切换；左右滑动留给首页内部的
            // 推荐/直播/番剧分段，避免一次横划同时牵动两套分页。
            userScrollEnabled = false,
            // 到头/到尾再滑不出现任何回弹动画
            overscrollEffect = null,
        ) { page ->
            when (page) {
                0 -> androidx.compose.runtime.key(settings.homeChannels, settings.homeChannelOrder) { HomeSectionPager(
                    homeSection = homeSection,
                    onSectionChange = onHomeSectionChange,
                    onSectionPositionChange = onHomeSectionPositionChange,
                    homeFeed = homeFeed,
                    session = session,
                    settings = settings,
                    topBarHeight = topBarHeight,
                    interactive = homeInteractive && pager.settledPage == 0 && !pager.isScrollInProgress,
                    context = context,
                ) }

                1 -> FeedScreen(
                    loggedIn = session != null,
                    onNeedLogin = { navigateToLogin(context) },
                    onOpenVideo = { navigateToVideo(context, it) },
                    onOpenFeed = { navigateToFeed(context, it) },
                    refreshTick = dynamicRefreshTick,
                    initialTab = dynamic.tab,
                    tabClickTick = dynamic.clickTick,
                    onTabChange = dynamic.onTabChange,
                    onTabPositionChange = dynamic.onPositionChange,
                )

                else -> MainMineTab(session, settings, mineList)
            }
        }
    }
}

/**
 * 首页五段：推荐 / 直播 / 番剧 / 热门 / 影视。
 *
 * 和底栏主 tab 一样用 HorizontalPager，但这里才是手指左右划动切换的地方；
 * 顶部胶囊点选与滑动共用同一个 pager 状态，两边的选中态不会打架。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HomeSectionPager(
    homeSection: Int,
    onSectionChange: (Int) -> Unit,
    onSectionPositionChange: (Float) -> Unit,
    homeFeed: com.aritxonly.bdfun.feature.home.presentation.HomeFeedState,
    session: UserProfile?,
    settings: SettingsBindings,
    topBarHeight: androidx.compose.ui.unit.Dp,
    interactive: Boolean,
    context: android.content.Context,
) {
    val channels = com.aritxonly.bdfun.core.domain.HomeChannelPolicy.ordered(
    settings.homeChannels, settings.homeChannelOrder,
)
    val latestSection by androidx.compose.runtime.rememberUpdatedState(homeSection)
    val latestOnSectionChange by androidx.compose.runtime.rememberUpdatedState(onSectionChange)
    val latestOnPositionChange by androidx.compose.runtime.rememberUpdatedState(onSectionPositionChange)
    val sectionPager = rememberPagerState(
        initialPage = channels.indexOfFirst { it.ordinal == homeSection }.coerceAtLeast(0),
        pageCount = { channels.size },
    )
    // 顶栏点选触发的滚动期间，不要把手滑经过的中间页当成用户选择同步回去。
    var programmatic by remember { mutableStateOf(false) }
    // 顶部胶囊点选：只改 shell 状态，这里统一负责动画过去。
    LaunchedEffect(homeSection) {
        val target = channels.indexOfFirst { it.ordinal == homeSection }.coerceAtLeast(0)
        if (sectionPager.currentPage != target || sectionPager.currentPageOffsetFraction != 0f) {
            programmatic = true
            try {
                // 和小米文件管理同一条：350ms 三次方减速，点 tab 与手滑一致。
                sectionPager.animateScrollToPage(
                    target,
                    animationSpec = TeakPageSwitchSpec(),
                )
            } finally {
                programmatic = false
            }
        }
    }
    // 手指划到哪一段，就把选中态同步回 shell（顶栏胶囊、回到顶部、刷新入口都读它）。
    LaunchedEffect(sectionPager) {
        snapshotFlow { sectionPager.currentPage }.collect { page ->
            val id = channels[page].ordinal
            if (!programmatic && id != latestSection) latestOnSectionChange(id)
        }
    }
    // 顶栏指示器要跟手指走：把 pager 的连续位置每帧同步给 shell，而不是等落页才跳。
    LaunchedEffect(sectionPager) {
        snapshotFlow {
            (sectionPager.currentPage + sectionPager.currentPageOffsetFraction).coerceIn(0f, channels.lastIndex.toFloat())
        }.collect { latestOnPositionChange(it) }
    }
    HorizontalPager(
        state = sectionPager,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 0,
        overscrollEffect = null,
    ) { page ->
        val section = channels[page].ordinal
        HomeSectionContent(section, homeFeed, session, settings, topBarHeight, interactive, context)
    }
}

/** 页面切换统一使用文件管理同款弹簧；保留常量给动态页引用。 */
internal const val MainPageTransitionMs = 400

@Composable
private fun HomeSectionContent(
    section: Int,
    homeFeed: com.aritxonly.bdfun.feature.home.presentation.HomeFeedState,
    session: UserProfile?, settings: SettingsBindings, topBarHeight: androidx.compose.ui.unit.Dp,
    interactive: Boolean, context: android.content.Context,
) {
    when (section) {
        1 -> com.aritxonly.bdfun.presentation.live.LivePage(
            state = AppGraph.livePageState,
            loggedIn = session != null,
            topBarHeight = topBarHeight,
        )
        2 -> com.aritxonly.bdfun.presentation.popular.PopularPage(
            state = AppGraph.popularPageState,
            topBarHeight = topBarHeight,
        )
        3 -> com.aritxonly.bdfun.presentation.pgc.BangumiPage(
            state = AppGraph.bangumiPageState,
            topBarHeight = topBarHeight,
        )
        4 -> com.aritxonly.bdfun.presentation.pgc.CinemaPage(
            state = AppGraph.cinemaPageState,
            topBarHeight = topBarHeight,
        )
        else -> HomeScreenRoute(
            state = homeFeed,
            onOpenVideo = { navigateToVideo(context, it) },
            cardStyle = settings.homeCardStyle,
            interactive = interactive,
            topBarHeight = topBarHeight,
        )
    }
}

@Composable
private fun MainMineTab(session: UserProfile?, settings: SettingsBindings, mineList: androidx.compose.foundation.lazy.LazyListState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    MineScreen(
        com.aritxonly.bdfun.ui.MineScreenRequest(session, settings.sentHistoryEntry, AppGraph.session.identity.revision,
            AppGraph.session.refreshing, AppGraph.session.identity.mid != null),
        com.aritxonly.bdfun.ui.MineNavigation(
            { navigateToLogin(context) }, { navigateToSettings(context) },
            { navigateToLibrary(context) }, { navigateToHistory(context) },
            { navigateToWatchLater(context) }, { navigateToCache(context) },
            { navigateToMySent(context) }, { navigateToSpace(context, it) },
            { mid, fans -> RelationActivity.open(context, mid, fans) }, { navigateToVideo(context, it) },
        ),
        AppGraph.mineDependencies, mineList,
    )
}
