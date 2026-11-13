package com.aritxonly.bdfun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem

internal class MainShellUiStateHolder {
    var showSearch by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var searchSubmitted by mutableStateOf<String?>(null)
    var dynamicRefreshTick by mutableIntStateOf(0)
    // 首页顶部分段：0 推荐 / 1 直播 / 2 番剧。只在 HyperOS 4 风格下由顶栏切换。
    // 放在 shell 而不是 feature:home，是因为顶栏（shell chrome）和页面内容（pager）分属两层，
    // 中间隔了一次组合，没有共同的父级可以提升状态。
    var homeSection by mutableIntStateOf(0)

    fun closeSearch() { showSearch = false; searchQuery = ""; searchSubmitted = null }
}
internal data class MainShellNavigation(
    val pager: androidx.compose.foundation.pager.PagerState, val destinations: List<Destination>,
    val items: List<TeakFloatingTabItem>, val scope: kotlinx.coroutines.CoroutineScope,
)
internal data class MainShellChrome(
    val home: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    val dynamic: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    val search: com.aritxonly.teak.common.chrome.TeakPageScrollState,
)
internal data class MainShellFeeds(
    val session: com.aritxonly.bdfun.core.model.user.UserProfile?,
    val home: com.aritxonly.bdfun.feature.home.presentation.HomeFeedState,
    val dynamic: com.aritxonly.bdfun.feature.dynamic.presentation.DynamicFeedState,
    val unreadMessageCount: Int,
)
internal data class MainShellBackdrops(
    val main: top.yukonga.miuix.kmp.blur.LayerBackdrop,
    val search: top.yukonga.miuix.kmp.blur.LayerBackdrop, val wideNavigation: Boolean,
)
internal data class MainShellState(
    val ui: MainShellUiStateHolder, val navigation: MainShellNavigation, val chrome: MainShellChrome,
    val feeds: MainShellFeeds, val backdrops: MainShellBackdrops, val searchReveal: Float,
    val bottom: BottomSearchShellStateHolder,
    val searchFactory: () -> com.aritxonly.bdfun.feature.search.application.SearchStateHolder,
)
