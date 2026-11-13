package com.aritxonly.bdfun

import androidx.compose.foundation.layout.PaddingValues
import com.aritxonly.teak.common.chrome.centeredListWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@Composable
internal fun SpaceContentPager(
    content: SpaceScreenContent, layout: SpaceScreenLayout, actions: SpaceScreenActions, modifier: Modifier,
) {
    val pagerState = content.pagerState
    val wideSpace = layout.wideSpace
    val statusBarTop = layout.statusBarTop
    val tabListStates = layout.tabListStates
    val headerScrollConnection = layout.headerScrollConnection
    androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = modifier,
            // 相邻页也保持组合，滑过去时内容已经就位。
            beyondViewportPageCount = 1,
            overscrollEffect = null,
        ) { page ->
            androidx.compose.runtime.key(wideSpace, page) {
            LazyColumn(
                state = tabListStates[page.coerceIn(0, 3)],
                modifier = Modifier
                    .fillMaxSize()
                    .centeredListWidth(maxWidth = if (wideSpace) 1000.dp else 680.dp)
                    .nestedScroll(headerScrollConnection),
                // 「主页」内容本来就短，再留底部空白会被误认为还能继续滑一截。
                contentPadding = PaddingValues(
                    top = if (wideSpace) statusBarTop + 56.dp else 0.dp,
                    bottom = 40.dp,
                ),
                overscrollEffect = null,
            ) {
        when (page) {
            0 -> spaceHomeItems(content, actions)
            1 -> spaceFeedItems(content, actions)
            2 -> spaceSubmissionItems(content, actions)
            else -> spaceShopItems(content, actions)
        }
    }
            }
    }
}
