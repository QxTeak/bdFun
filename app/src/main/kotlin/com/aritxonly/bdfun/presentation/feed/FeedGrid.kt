package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
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
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.feed.FeedItem
import kotlin.math.roundToInt

@Composable
internal fun FeedGrid(
    state: FeedState, statusTop: Dp, pull: FeedPullStateHolder,
    connection: NestedScrollConnection, actions: FeedActions, media: FeedMedia,
    ring: FeedAvatarRingState,
    onSelectUp: (Long) -> Unit,
    onExitUp: () -> Unit,
    /**
     * 退让层用：转场期间要画「点开 UP 之前那一份列表」和它自己的滚动位置。
     * 两列表（退让层 + UP 页）同时在场时不能共用一个 LazyStaggeredGridState。
     */
    items: List<FeedItem> = state.items,
    listState: LazyStaggeredGridState = state.listState,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 736.dp
        // 直播 UP 集合在重组之间复用：动态列表每次分页/点赞都会重建 item 范围，
        // 每重建一次就 new 一个 HashSet 是白花。
        val liveMids = remember(state.liveUsers) {
            state.liveUsers.mapTo(HashSet()) { it.mid }
        }
        val columns = remember(wide) {
            if (wide) BoundedVideoStaggeredGridCells(FeedCardMinWidth.dp, FeedCardMaxWidth.dp)
            else StaggeredGridCells.Fixed(1)
        }
        val sidebarWidth = dynamicSidebarWidth(maxWidth.value).dp
        Row(Modifier.fillMaxSize()) {
            if (wide) FeedAuthorSidebar(
                state,
                modifier = Modifier.width(sidebarWidth),
                topPadding = statusTop + FeedTopBarHeight,
                bottomPadding = 100.dp,
            )
            FeedCardsGrid(
                state = state,
                items = items,
                actions = actions,
                media = media,
                liveMids = liveMids,
                listState = listState,
                columns = columns,
                contentPadding = PaddingValues(
                    start = TeakUiStyle.design.contentInset(8.dp),
                    // 固定顶栏显示分类分段栏；头像卡片已经放进列表里，会跟着一起滚走
                    top = statusTop + FeedTopBarHeight + 8.dp,
                    end = TeakUiStyle.design.contentInset(8.dp),
                    bottom = 100.dp,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .offset { IntOffset(0, pull.pull.roundToInt()) }
                    .nestedScroll(connection),
                showSkeleton = state.items.isEmpty() && state.loading,
                showFooter = true,
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
            }
        }
    }
}

/**
 * 动态卡片网格。UP 模式的 pager 页和普通列表共用这一份：
 * 分页 footer / 骨架屏由调用方决定，普通页和预览页就不会各写一套卡片布局。
 */
@Composable
internal fun FeedCardsGrid(
    state: FeedState,
    items: List<FeedItem>,
    actions: FeedActions,
    media: FeedMedia,
    liveMids: Set<Long>,
    listState: LazyStaggeredGridState,
    columns: StaggeredGridCells,
    contentPadding: PaddingValues,
    modifier: Modifier,
    showSkeleton: Boolean,
    showFooter: Boolean,
    header: (LazyStaggeredGridScope.() -> Unit)? = null,
) {
    LazyVerticalStaggeredGrid(
        columns = columns,
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalItemSpacing = TeakUiStyle.design.itemSpacing(10.dp, 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        header?.invoke(this)
        // 动态首屏还在加载时先铺卡片骨架，避免空白页只挂一个小电视。
        if (showSkeleton) dynamicFeedSkeletonItems()
        dynamicFeedItems(state, items, actions, media, liveMids)
        if (showFooter) dynamicFeedFooter(state, actions)
    }
}
