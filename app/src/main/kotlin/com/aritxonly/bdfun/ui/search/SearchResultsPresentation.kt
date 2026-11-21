package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class SearchResultsPresentation(
    val pager: androidx.compose.foundation.pager.PagerState, val position: androidx.compose.runtime.State<Float>,
    val videos: androidx.compose.foundation.lazy.grid.LazyGridState,
    val bangumi: androidx.compose.foundation.lazy.grid.LazyGridState,
    val films: androidx.compose.foundation.lazy.grid.LazyGridState,
    val users: androidx.compose.foundation.lazy.grid.LazyGridState,
    val backdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop,
) {
    /** Tab 顺序：视频 / 番剧 / 影视 / 用户。 */
    val listStates get() = listOf(videos, bangumi, films, users)
}

@Composable
internal fun rememberSearchResultsPresentation(tab: Int, onTabChange: (Int) -> Unit): SearchResultsPresentation {
    val pagerState = rememberPagerState(pageCount = { 4 })
    LaunchedEffect(tab) {
        if (pagerState.currentPage != tab) pagerState.animateScrollToPage(tab)
    }
    // 只认稳定落页，避免从「视频」快速点到「用户」时，把动画中间页写回 tab 造成两页各露一半。
    // 不能在 effect 启动时拿当前 settledPage 和 tab 比较：点 Tab 的瞬间两者必然不同，
    // 会把刚设置的 tab 立刻改回去，动画还没开始就被取消了。
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect(onTabChange)
    }
    // 指示器跟随手指：左右滑时不是等松手才跳过去。
    val position = remember {
        derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction }
    }
    val videoListState = rememberLazyGridState()
    val bangumiListState = rememberLazyGridState()
    val filmsListState = rememberLazyGridState()
    val userListState = rememberLazyGridState()
    val states = listOf(videoListState, bangumiListState, filmsListState, userListState)
    TrackTeakPageScroll(states[pagerState.currentPage.coerceIn(0, states.lastIndex)])
    // 给结果内容单独录一层：Tab 栏是它的兄弟节点，不在录制范围内，
    // 因此 Tab 的玻璃可以安全采样，不会形成自引用。
    val colors = MiuixTheme.colorScheme
    val tabBackdrop = rememberLayerBackdrop {
        drawRect(colors.background)
        drawContent()
    }

    return SearchResultsPresentation(
        pagerState, position, videoListState, bangumiListState, filmsListState, userListState, tabBackdrop,
    )
}
