package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlin.math.roundToInt

@Composable
internal fun HomeVideoGrid(
    state: HomeFeedState, largeCards: Boolean,
    width: androidx.compose.ui.unit.Dp, statusBarTop: androidx.compose.ui.unit.Dp,
    ui: HomePresentationStateHolder, refreshConnection: NestedScrollConnection,
    scope: kotlinx.coroutines.CoroutineScope, onOpenVideo: (VideoItem) -> Unit, host: HomePresentationHost,
) {
    LazyVerticalGrid(
        columns = if (width >= 600.dp) host.gridCells(180.dp, 260.dp) else GridCells.Fixed(if (largeCards) 1 else 2),
        state = state.gridState,
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, ui.pullOffset.roundToInt()) }
            .nestedScroll(refreshConnection),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = host.topBarHeight + statusBarTop + 6.dp,
            end = 12.dp,
            // 让最后一排卡片能滚出悬浮底栏
            bottom = 104.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.loading && state.videos.isEmpty()) {
            homeFeedSkeletonItems()
        }
        homeVideoItems(state, largeCards, ui, onOpenVideo, host)
        if (state.videos.isNotEmpty()) {
            item(key = "footer", span = { GridItemSpan(maxLineSpan) }) { HomeFeedFooter(state, scope) }
        }
    }

}
