package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

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
import androidx.compose.runtime.remember
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
    ring: FeedAvatarRingState,
    onSelectUp: (Long) -> Unit,
    onExitUp: () -> Unit,
    transition: FeedUpPageTransition,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 736.dp
        // 直播 UP 集合在重组之间复用：动态列表每次分页/点赞都会重建 item 范围，
        // 每重建一次就 new 一个 HashSet 是白花。
        val liveMids = remember(state.liveUsers) {
            state.liveUsers.mapTo(HashSet()) { it.mid }
        }

        val sidebarWidth = dynamicSidebarWidth(maxWidth.value).dp
        Row(Modifier.fillMaxSize()) {
            if (wide) FeedAuthorSidebar(
                state,
                modifier = Modifier.width(sidebarWidth),
                topPadding = statusTop + FeedTopBarHeight,
                bottomPadding = 100.dp,
            )
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
                    start = TeakUiStyle.design.contentInset(8.dp),
                    // 固定顶栏显示分类分段栏；头像卡片已经放进列表里，会跟着一起滚走
                    top = statusTop + FeedTopBarHeight + 8.dp,
                    end = TeakUiStyle.design.contentInset(8.dp),
                    bottom = 100.dp,
                ),
                verticalItemSpacing = TeakUiStyle.design.itemSpacing(10.dp, 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                if (!wide) item(key = "avatar-card", span = StaggeredGridItemSpan.FullLine) {
                    FeedAvatarCard(
                        state = state,
                        ring = ring,
                        onSelectUp = onSelectUp,
                        onExitUp = onExitUp,
                        modifier = Modifier.padding(bottom = TeakUiStyle.design.contentInset(2.dp)),
                    )
                }
                // 动态首屏还在加载时先铺卡片骨架，避免空白页只挂一个小电视。
                if (state.items.isEmpty() && state.loading) {
                    dynamicFeedSkeletonItems()
                }
                dynamicFeedItems(state, actions, media, liveMids, transition)
                dynamicFeedFooter(state, actions)
            }

        }
    }
}
