package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TeakScrollContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.VideoItem
import kotlinx.coroutines.flow.drop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchResultList(
    page: Int, presentation: SearchResultsPresentation,
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    onOpenVideo: (VideoItem) -> Unit, onOpenUser: (SearchUser) -> Unit,
    onLoadMore: () -> Unit = {},
) {
    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val pageColors = MiuixTheme.colorScheme
    val listState = if (page == 0) presentation.videos else presentation.users
    // 只有视频 Tab 有分页：滑到接近底部就拉下一页；drop(1) 忽略进入页面时的首次布局。
    // 结果数量用 rememberUpdatedState 读最新值，否则 effect 会一直拿第一次的 20 条判断阈值。
    val resultsSize by androidx.compose.runtime.rememberUpdatedState(content.results.size)
    val loadMore by androidx.compose.runtime.rememberUpdatedState(onLoadMore)
    if (page == 0) {
        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                .drop(1)
                .collect { last ->
                    if (resultsSize > 0 && last >= resultsSize - 4) loadMore()
                }
        }
    }
    TeakScrollContainer(listState, Modifier.fillMaxSize()) {
    LazyVerticalGrid(
        columns = if (wide) BoundedVideoGridCells(280.dp, 420.dp) else GridCells.Fixed(1),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            // 顶栏 + Tab 栏 + 一点间距
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                HomeTopBarHeight + if (useSingleRowSearchResults()) 6.dp else 54.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        searchResultStatusItems(content, pageColors)
        if (page == 0) {
            searchVideoResultItems(content, pageColors, onOpenVideo)
            searchVideoPageFooter(content, pageColors)
        } else {
            searchUserResultItems(content, pageColors, onOpenUser)
        }

    }
    }
}
