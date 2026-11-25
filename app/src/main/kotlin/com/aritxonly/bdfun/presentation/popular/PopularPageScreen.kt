package com.aritxonly.bdfun.presentation.popular

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.presentation.common.discover.DiscoverPullUi
import com.aritxonly.bdfun.ui.BoundedVideoGridCells
import com.aritxonly.bdfun.ui.HomeTopBarHeight
import com.aritxonly.bdfun.ui.LocalVideoCardMorphMenu
import com.aritxonly.bdfun.ui.rememberVideoCardMorphMenuHandler
import com.aritxonly.bdfun.ui.rememberVideoShareMorphItems
import com.aritxonly.bdfun.ui.VideoActionMenu
import com.aritxonly.bdfun.ui.VideoMenuItem
import com.aritxonly.bdfun.ui.showToast
import com.aritxonly.bdfun.ui.videoResultFooter
import com.aritxonly.bdfun.ui.videoResultRows
import com.aritxonly.bdfun.ui.videoResultSkeleton
import com.aritxonly.teak.common.chrome.TeakScrollContainer
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * 首页第二段「热门」。
 *
 * 列表外观复用搜索结果的视频行；下拉刷新、回顶与收尾时序和推荐 / 直播 / 番剧
 * 共用 `PullRefreshSession` + `RefreshToTopEffect` + `PullRefreshSettleEffect`，
 * 小电视指示器由 App 层的 `MainHomeChrome` 统一绘制。
 */
@Composable
internal fun PopularPage(
    state: PopularPageStateHolder,
    topBarHeight: Dp = HomeTopBarHeight,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 省略号 / 长按统一走 Activity 根部的形变菜单，和首页同一套动画。
    val morphMenu = rememberVideoCardMorphMenuHandler(rememberVideoShareMorphItems())
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
    val pullUi = remember { DiscoverPullUi() }
    val pull = rememberPopularPullSession(state, pullUi)
    PopularPageEffects(state, pull, pullUi)

    androidx.compose.runtime.CompositionLocalProvider(LocalVideoCardMorphMenu provides morphMenu) {
    TeakScrollContainer(gridState, Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = if (wide) BoundedVideoGridCells(280.dp, 420.dp) else GridCells.Fixed(1),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, pullUi.offset.roundToInt()) }
                .nestedScroll(pull.connection()),
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
                        PopularStatus("$message · 点击重试", colors.onBackgroundVariant) {
                            scope.launch { state.retry() }
                        }
                    }
                }
            }
            videoResultRows(
                videos, "popular-",
                onOpenVideo = { navigateToVideo(context, it) },
            )
            if (loadedOnce && !loading && videos.isEmpty() && error == null) {
                item(key = "popular-empty", span = { GridItemSpan(maxLineSpan) }) {
                    PopularStatus("暂时没有热门内容", colors.onBackgroundVariant)
                }
            }
            videoResultFooter(loadingMore = loadingMore, end = end, colors = colors)
        }
    }
    }
}

@Composable
private fun PopularStatus(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null,
) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        modifier = Modifier
            .padding(top = 24.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    )
}

@Composable
private fun rememberPopularPullSession(
    state: PopularPageStateHolder,
    ui: DiscoverPullUi,
): com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession {
    val density = LocalDensity.current
    return remember(state) {
        com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession(
            thresholdPx = with(density) { 88.dp.toPx() },
            maxPullPx = with(density) { 260.dp.toPx() },
            density = density,
            currentOffset = { ui.offset },
            onPull = { px, dp, fraction ->
                ui.offset = px
                state.pullOffsetDp = dp
                state.pullFraction = fraction
            },
            canScrollBackward = { state.gridState.canScrollBackward },
            isRefreshing = { state.refreshing || state.loading },
            onRefresh = {
                // 与推荐页一致：松手立即把指示器顶到阈值，再开始刷新。
                state.pullFraction = 1f
                state.refresh()
            },
            onCanceled = { state.pullFraction = 0f },
        )
    }
}

@Composable
private fun PopularPageEffects(
    state: PopularPageStateHolder,
    pull: com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession,
    ui: DiscoverPullUi,
) {
    LaunchedEffect(state) { state.ensureLoaded() }
    // 再次点击底栏「首页」：和直播/番剧一样走同一份“回顶 + 下拉 + 刷新”编排。
    com.aritxonly.bdfun.feature.home.presentation.RefreshToTopEffect(
        request = state.refreshRequest,
        pullThresholdPx = pull.thresholdPx,
        animatePull = { target, duration -> pull.animatePullTo(target, duration) },
        isBusy = { state.refreshing || state.loading },
        onPulling = { state.pullFraction = it },
        isAtTop = {
            state.gridState.firstVisibleItemIndex == 0 &&
                state.gridState.firstVisibleItemScrollOffset == 0
        },
        scrollToTop = { state.gridState.animateScrollToItem(0) },
        refresh = { state.refresh() },
    )
    com.aritxonly.bdfun.feature.home.presentation.PullRefreshSettleEffect(
        refreshing = state.refreshing,
        currentOffset = { ui.offset },
        onSettledStart = { state.pullFraction = 0f },
        animatePull = { target, duration -> pull.animatePullTo(target, duration) },
    )
    // 滑到底部预取下一页；用 rememberUpdatedState 读最新数量，避免 effect 停在首屏。
    val size by rememberUpdatedState(state.videos.size)
    val canLoadMore by rememberUpdatedState(!state.end && !state.loadingMore && !state.loading)
    LaunchedEffect(state.gridState) {
        snapshotFlow { state.gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .drop(1)
            .collect { last ->
                if (canLoadMore && size > 0 && last >= size - 4) state.loadMore()
            }
    }
}
