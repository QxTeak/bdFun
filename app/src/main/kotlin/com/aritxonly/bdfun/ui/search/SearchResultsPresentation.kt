package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class SearchResultsPresentation(
    val pager: androidx.compose.foundation.pager.PagerState, val position: androidx.compose.runtime.State<Float>,
    val videos: androidx.compose.foundation.lazy.grid.LazyGridState, val users: androidx.compose.foundation.lazy.grid.LazyGridState,
    val backdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop,
)

@Composable
internal fun rememberSearchResultsPresentation(tab: Int, onTabChange: (Int) -> Unit): SearchResultsPresentation {
    val pagerState = rememberPagerState(pageCount = { 2 })
    LaunchedEffect(tab) {
        if (pagerState.currentPage != tab) pagerState.animateScrollToPage(tab)
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != tab) onTabChange(pagerState.currentPage)
    }
    // 指示器跟随手指：左右滑时不是等松手才跳过去。
    val position = remember {
        derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction }
    }
    val videoListState = rememberLazyGridState()
    val userListState = rememberLazyGridState()
    TrackTeakPageScroll(if (pagerState.currentPage == 0) videoListState else userListState)
    // 给结果内容单独录一层：Tab 栏是它的兄弟节点，不在录制范围内，
    // 因此 Tab 的玻璃可以安全采样，不会形成自引用。
    val colors = MiuixTheme.colorScheme
    val tabBackdrop = rememberLayerBackdrop {
        drawRect(colors.background)
        drawContent()
    }

    return SearchResultsPresentation(pagerState, position, videoListState, userListState, tabBackdrop)
}
