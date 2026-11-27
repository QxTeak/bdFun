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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
internal fun FeedContent(
    state: FeedState, statusTop: Dp,
    pull: FeedPullStateHolder, connection: NestedScrollConnection,
    actions: FeedActions,
    ring: FeedAvatarRingState,
    onSelectUp: (Long) -> Unit,
    onExitUp: () -> Unit,
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
            // 滚动过程中不计算最近项：每帧 minBy 的结果一定会被丢掉。
            if (state.listState.isScrollInProgress) return@snapshotFlow null
            val info = state.listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo.minByOrNull {
                kotlin.math.abs(it.offset.y + it.size.height / 2f - center)
            }?.key
        }.filterNotNull().distinctUntilChanged().collect { index ->
            // Stable item keys identify the nearest visible card independently of its lane.
            ui.autoPlayId = visible.firstOrNull { it.id == index }
                ?.takeIf { it.video != null || it.orig?.video != null }
                ?.id
        }
    }

    // onToggleMute 必须是稳定引用：它一变，动态卡片的 cardActions remember 就会整排失效、
    // 所有可见卡片一起重组。用 rememberUpdatedState 读最新值，lambda 只建一次。
    val mutedState = androidx.compose.runtime.rememberUpdatedState(autoPlayMuted)
    val onToggleMute = remember {
        { AppGraph.settings.dynamicAutoPlayMuted = !mutedState.value }
    }
    val media = remember(dynamicAutoPlay, ui.autoPlayId, autoPlayMuted, onToggleMute) {
        FeedMedia(dynamicAutoPlay, ui.autoPlayId, autoPlayMuted, onToggleMute)
    }
    FeedGrid(state, statusTop, pull, connection, actions, media, ring, onSelectUp, onExitUp)
}
