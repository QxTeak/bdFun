package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun FeedGrid(
    state: FeedState, statusTop: Dp, pull: FeedPullStateHolder,
    connection: NestedScrollConnection, actions: FeedActions, media: FeedMedia,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 736.dp
        val sidebarWidth = dynamicSidebarWidth(maxWidth.value).dp
        Row(Modifier.fillMaxSize()) {
            if (wide) FeedAuthorSidebar(state, Modifier.width(sidebarWidth)
                .padding(top = statusTop + FeedTopBarHeight, bottom = 100.dp))
            LazyVerticalStaggeredGrid(
                columns = if (wide) BoundedVideoStaggeredGridCells(FeedCardMinWidth.dp, FeedCardMaxWidth.dp)
                    else androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Fixed(1),
                state = state.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)

                    .offset { IntOffset(0, pull.pull.roundToInt()) }
                    .nestedScroll(connection),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    // 固定顶栏显示分类分段栏；头像卡片已经放进列表里，会跟着一起滚走
                    top = statusTop + FeedTopBarHeight + 8.dp,
                    end = 8.dp,
                    bottom = 100.dp,
                ),
                verticalItemSpacing = 10.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                if (!wide) item(key = "avatar-card", span = StaggeredGridItemSpan.FullLine) {
                    FeedAvatarCard(
                        state = state,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                dynamicFeedItems(state, actions, media)
                dynamicFeedFooter(state, actions)
            }

        }
    }
}
