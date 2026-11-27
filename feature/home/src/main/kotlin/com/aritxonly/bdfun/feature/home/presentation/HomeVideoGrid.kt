package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.theme.TeakStyle
import com.aritxonly.teak.common.theme.TeakUiStyle

@Composable
internal fun HomeVideoGrid(
    state: HomeFeedState, largeCards: Boolean,
    width: androidx.compose.ui.unit.Dp, statusBarTop: androidx.compose.ui.unit.Dp,
    ui: HomePresentationStateHolder, refreshConnection: NestedScrollConnection,
    scope: kotlinx.coroutines.CoroutineScope, onOpenVideo: (VideoItem) -> Unit, host: HomePresentationHost,
) {
    // 哔哩哔哩布局的分段 Tab 和首行卡片之间再多留 5dp，和原版间距对齐。
    val sectionGap = if (TeakUiStyle.current == TeakStyle.Official) 5.dp else 0.dp
    // 列定义与内边距只在宽度/样式变化时重建，避免每次重组都让整张网格重新测量。
    val columns = remember(width, largeCards, host) {
        if (width >= 600.dp) host.gridCells(180.dp, 260.dp) else GridCells.Fixed(if (largeCards) 1 else 2)
    }
    val contentPadding = remember(host, statusBarTop, sectionGap) {
        PaddingValues(
            start = 12.dp,
            top = host.topBarHeight + statusBarTop + 6.dp + sectionGap,
            end = 12.dp,
            // 让最后一排卡片能滚出悬浮底栏
            bottom = 104.dp,
        )
    }
    LazyVerticalGrid(
        columns = columns,
        state = state.gridState,
        modifier = Modifier
            .fillMaxSize()
            // 下拉位移只改绘制层：用 offset{} 会让整张 LazyGrid 每帧重新测量/摆放。
            .graphicsLayer { translationY = ui.pullOffset }
            .nestedScroll(refreshConnection),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.loading && state.videos.isEmpty()) {
            // contentPadding.top 会让加载项整体下移，这里把同样的距离补回给高度计算，
            // 让加载图标重新回到屏幕正中。
            homeFeedSkeletonItems(
                extraTopInset = host.topBarHeight + statusBarTop + 12.dp + sectionGap * 2,
            )
        }
        homeVideoItems(state, largeCards, ui, onOpenVideo, host)
        if (state.videos.isNotEmpty()) {
            item(key = "footer", span = { GridItemSpan(maxLineSpan) }) { HomeFeedFooter(state, scope) }
        }
    }

}
