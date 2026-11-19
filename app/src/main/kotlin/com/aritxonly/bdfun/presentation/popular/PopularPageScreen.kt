package com.aritxonly.bdfun.presentation.popular

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.ui.BoundedVideoGridCells
import com.aritxonly.bdfun.ui.HomeTopBarHeight
import com.aritxonly.bdfun.ui.videoResultFooter
import com.aritxonly.bdfun.ui.videoResultRows
import com.aritxonly.bdfun.ui.videoResultSkeleton
import com.aritxonly.teak.common.chrome.TeakScrollContainer
import kotlinx.coroutines.flow.drop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页第二段「热门」。
 *
 * 列表外观直接复用搜索结果的视频行（`videoResultRows` 等），
 * 这里只维护热门接口的分页、错误和刷新；顶栏渐进模糊由 shell 的 TrackTeakPageScroll 接管。
 */
@Composable
internal fun PopularPage(
    state: PopularPageStateHolder,
    topBarHeight: Dp = HomeTopBarHeight,
) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val gridState = state.gridState
    val videos = state.videos
    val loading = state.loading
    val error = state.error
    val loadingMore = state.loadingMore
    val end = state.end
    val loadedOnce = state.loadedOnce

    LaunchedEffect(state) { state.ensureLoaded() }
    // 再次点击底栏「首页」：先回顶，再重拉第一页（与直播/番剧同一入口语义）。
    LaunchedEffect(state.refreshRequest) {
        if (state.refreshRequest == 0) return@LaunchedEffect
        if (!state.isAtTop()) gridState.animateScrollToItem(0)
        state.reload()
    }
    // 滑到接近底部预取下一页；用 rememberUpdatedState 读最新数量，避免 effect 停在首屏。
    val size by rememberUpdatedState(videos.size)
    val canLoadMore by rememberUpdatedState(!end && !loadingMore && !loading)
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .drop(1)
            .collect { last ->
                if (canLoadMore && size > 0 && last >= size - 4) state.loadMore()
            }
    }

    TeakScrollContainer(gridState, Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = if (wide) BoundedVideoGridCells(280.dp, 420.dp) else GridCells.Fixed(1),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = statusBarTop + topBarHeight + 6.dp,
                bottom = 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading && videos.isEmpty()) {
                videoResultSkeleton(keyPrefix = "popular-skeleton")
            }
            error?.let { message ->
                if (videos.isEmpty()) {
                    item(key = "popular-error", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "$message · 点击重试",
                            color = colors.onBackgroundVariant,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .clickable { state.requestRefresh() },
                        )
                    }
                }
            }
            videoResultRows(videos, "popular-", onOpenVideo = { navigateToVideo(context, it) })
            if (loadedOnce && !loading && videos.isEmpty() && error == null) {
                item(key = "popular-empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "暂时没有热门内容",
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
            videoResultFooter(loadingMore = loadingMore, end = end, colors = colors)
        }
    }
}
