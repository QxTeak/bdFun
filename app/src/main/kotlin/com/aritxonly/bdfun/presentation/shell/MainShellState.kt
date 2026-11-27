package com.aritxonly.bdfun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem

internal class MainShellUiStateHolder {
    var showSearch by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var searchSubmitted by mutableStateOf<String?>(null)
    var dynamicRefreshTick by mutableIntStateOf(0)
    // 首页顶部分段：0 推荐 / 1 直播 / 2 番剧 / 3 热门 / 4 影视。
    // 放在 shell 而不是 feature:home，是因为顶栏（shell chrome）和页面内容（pager）分属两层，
    // 中间隔了一次组合，没有共同的父级可以提升状态。
    var homeSection by mutableIntStateOf(0)
    /** 首页内部分页的连续位置（0.0 推荐，1.0 直播，2.0 番剧）；顶栏指示器跟手指走。 */
    val homeSectionPosition = mutableFloatStateOf(0f)
    // 动态页顶部分类：shell 顶栏和 pager 内容同样分属两层，状态放这里。
    // 空串表示尚未初始化，由 shell 按用户排序落到第一个可见分类。
    var dynamicTab by mutableStateOf("")
    /** 顶部分段被点了一次（含点当前项）：动态页据此退出 UP 模式。 */
    var dynamicTabClickTick by mutableIntStateOf(0)
    val dynamicTabPosition = mutableFloatStateOf(0f)

    /**
     * 只关页面，不立即清空输入/结果状态：收起动画期间查询内容还要继续绘制，
     * 否则会先闪一下空白的搜索页再退出。下次打开时入口会主动重置。
     */
    fun closeSearch() { showSearch = false }
}
internal data class MainShellNavigation(
    val pager: androidx.compose.foundation.pager.PagerState, val destinations: List<Destination>,
    val items: List<TeakFloatingTabItem>, val scope: kotlinx.coroutines.CoroutineScope,
)
internal data class MainShellChrome(
    val home: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    val live: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    val bangumi: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    val cinema: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    val popular: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    val dynamic: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    val search: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    val mine: com.aritxonly.teak.common.chrome.TeakPageScrollState,
    val mineList: androidx.compose.foundation.lazy.LazyListState,
)
internal data class MainShellFeeds(
    val session: com.aritxonly.bdfun.core.model.user.UserProfile?,
    val home: com.aritxonly.bdfun.feature.home.presentation.HomeFeedState,
    val dynamic: com.aritxonly.bdfun.feature.feed.presentation.FeedState,
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
