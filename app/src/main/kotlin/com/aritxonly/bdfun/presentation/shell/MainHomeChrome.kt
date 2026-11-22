package com.aritxonly.bdfun

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.HomePullIndicator
import com.aritxonly.bdfun.ui.HomeTopBarHeight
import com.aritxonly.bdfun.ui.homeTopBarHeight

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainHomeChrome(
    settings: SettingsBindings,
    shell: MainShellState,
    pageFloat: Float,
) {
    val ui = shell.ui
    val homeFeed = shell.feeds.home
    // 推荐 / 直播 / 热门 / 番剧 / 影视 各自一份滚动进度，但顶栏和搜索过渡是同一份实现。
    val activeChrome = when (ui.homeSection) {
        1 -> shell.chrome.live
        2 -> shell.chrome.popular
        3 -> shell.chrome.bangumi
        4 -> shell.chrome.cinema
        else -> shell.chrome.home
    }
    // 下拉刷新指示器：各段各一份状态，都画在 App 层同一个位置。
    val liveState = if (ui.homeSection == 1) AppGraph.livePageState else null
    val popularState = if (ui.homeSection == 2) AppGraph.popularPageState else null
    val bangumiState = if (ui.homeSection == 3) AppGraph.bangumiPageState else null
    val cinemaState = if (ui.homeSection == 4) AppGraph.cinemaPageState else null
    val pullRefreshing = liveState?.refreshing ?: popularState?.refreshing
        ?: bangumiState?.refreshing ?: cinemaState?.refreshing ?: homeFeed.refreshing
    val pullOffsetDp = liveState?.pullOffsetDp ?: popularState?.pullOffsetDp
        ?: bangumiState?.pullOffsetDp ?: cinemaState?.pullOffsetDp ?: homeFeed.pullOffsetDp

    // 哔哩哔哩布局：分段条随手指方向收起/露出；HyperOS 4 的底栏搜索页不适用。
    val tabsAway by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (settings.interfaceStyle == 0 && !ui.showSearch) activeChrome.scrollAway else 0f,
        animationSpec = androidx.compose.animation.core.tween(220),
        label = "homeTabsAway",
    )
    if (!(ui.showSearch && ui.searchSubmitted != null && com.aritxonly.bdfun.ui.useSingleRowSearchResults())) MainTopBarChrome(
        shell = shell,
        pageIndex = 0,
        pageFloat = pageFloat,
        surface = activeChrome,
    ) {
        MainHomeBarContent(
            settings, shell,
            tabsCollapseProgress = tabsAway,
        )
    }

    if (pageFloat < 0.99f) {
        // 下拉刷新指示器：独立覆盖层，挂在 App 层，探出顶栏也不会被裁掉。
        // 透明度直接由下拉位移决定：位移 58dp 开始淡入、84dp 全显；
        // 卡片在位移 <46dp 时才会撞到小电视，因此永远追不上。
        HomePullIndicator(
            reveal = if (pullRefreshing) {
                1f
            } else {
                ((pullOffsetDp - 58f) / 26f).coerceIn(0f, 1f)
            },
            animated = pullRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = homeTopBarHeight(settings.interfaceStyle) + 4.dp),
        )
    }
}
