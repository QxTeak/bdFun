package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.AppGraph
import androidx.compose.runtime.snapshotFlow

@Composable
internal fun FeedContent(
    state: FeedState, statusTop: Dp,
    pull: FeedPullStateHolder, connection: NestedScrollConnection,
    actions: FeedActions,
) {
    val visible = state.items

    // 只在滚动停下来后判断：离屏幕中心最近的那条如果是视频，就静音自动播放。
    val ui = remember { FeedAutoPlayStateHolder() }
    val dynamicAutoPlay = AppGraph.settings.dynamicAutoPlay
    val autoPlayMuted = AppGraph.settings.dynamicAutoPlayMuted
    LaunchedEffect(state.listState, visible, dynamicAutoPlay) {
        if (!dynamicAutoPlay) {
            ui.autoPlayId = null
            return@LaunchedEffect
        }
        androidx.compose.runtime.snapshotFlow {
            val info = state.listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val nearest = info.visibleItemsInfo.minByOrNull {
                kotlin.math.abs(it.offset.y + it.size.height / 2f - center)
            }?.key
            state.listState.isScrollInProgress to nearest
        }.collect { (scrolling, index) ->
            if (scrolling) return@collect
            // Stable item keys identify the nearest visible card independently of its lane.
            ui.autoPlayId = visible.firstOrNull { it.id == index }
                ?.takeIf { it.video != null || it.orig?.video != null }
                ?.id
        }
    }

    val media = FeedMedia(dynamicAutoPlay, ui.autoPlayId, autoPlayMuted) {
        AppGraph.settings.dynamicAutoPlayMuted = !autoPlayMuted
    }
    FeedGrid(state, statusTop, pull, connection, actions, media)
}
