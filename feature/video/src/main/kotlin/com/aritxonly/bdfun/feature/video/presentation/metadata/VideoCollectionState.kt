package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal class VideoCollectionExpansionStateHolder {
    var expanded by mutableStateOf(false)
    /** 第一次展开时靠初始索引直接落在当前集，不需要滚动。 */
    var firstExpandDone by mutableStateOf(false)
}
internal class VideoCollectionScrollStateHolder {
    var pendingScrollTarget by mutableStateOf<Int?>(null)
}
internal class VideoCollectionPresentation(
    val expansion: VideoCollectionExpansionStateHolder,
    val scroll: VideoCollectionScrollStateHolder,
    val scrollState: LazyListState,
    val currentIndex: Int,
    val scrollTo: (Int) -> Unit,
    val keepInsideCollection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
)

@Composable
internal fun rememberVideoCollectionPresentation(
    title: String, episodes: List<com.aritxonly.bdfun.core.model.video.VideoEpisode>, currentCid: Long,
): VideoCollectionPresentation {
    val scope = rememberCoroutineScope()
    val expansion = remember(title, episodes.size) { VideoCollectionExpansionStateHolder() }
    val scroll = remember { VideoCollectionScrollStateHolder() }
    // 点击合集标题栏按钮时，等展开动画让列表完成测量后再滚。
    // 0 = 顶部，1 = 当前集，2 = 底部。

    val currentIndex = episodes.indexOfFirst { it.cid == currentCid }.let {
        if (it < 0) 0 else it
    }
    // 展开时让当前集直接出现在列表中间：初始索引往上留一条，
    // 由 LazyColumn 只组合可见的几行，而不是展开后再从列表头一路滑过来。
    val centeredIndex = (currentIndex - 1).coerceAtLeast(0)
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = centeredIndex)
    suspend fun snapToCurrent() {
        // 瞬时定位，不播放滚动动画。
        scrollState.scrollToItem(centeredIndex)
    }
    suspend fun scrollToTarget(target: Int?) {
        val index = videoCollectionScrollIndex(target, currentIndex, episodes.size) ?: return
        // LazyList按索引跳过不可见条目，不能再读取远处条目的绝对坐标。
        scrollState.animateScrollToItem(index)
        if (target != 0 && target != 2) {
            val layout = scrollState.layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: return
            val center = (layout.viewportStartOffset + layout.viewportEndOffset - item.size) / 2f
            scrollState.animateScrollBy(item.offset - center)
        }
    }
    // 展开时重活一次：首次展开由初始索引定位；之后每次展开和「定位到当前集」
    // 都直接跳到当前集，不再有“快速上下划动”的过程。
    LaunchedEffect(expansion.expanded) {
        if (!expansion.expanded) return@LaunchedEffect
        val target = scroll.pendingScrollTarget
        scroll.pendingScrollTarget = null
        if (!expansion.firstExpandDone) {
            expansion.firstExpandDone = true
            if (target == null || target == 1) return@LaunchedEffect
        }
        delay(40)
        if (target == null || target == 1) snapToCurrent() else scrollToTarget(target)
    }
    fun scrollCollection(target: Int) {
        scope.launch {
            if (!expansion.expanded) {
                scroll.pendingScrollTarget = target
                expansion.expanded = true
            } else if (target == 1) {
                snapToCurrent()
            } else {
                delay(40)
                scrollToTarget(target)
            }
        }
    }
    val keepInsideCollection = rememberVideoCollectionScrollConnection()
    return VideoCollectionPresentation(expansion, scroll, scrollState, currentIndex, ::scrollCollection, keepInsideCollection)
}

@Composable
private fun rememberVideoCollectionScrollConnection(): NestedScrollConnection {
    // Miuix 负责回弹，这一层负责把回弹之后剩下的位移/惯性吃掉，
    // 否则快速一甩仍会把速度传给整页。
    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = Offset(0f, available.y)

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = Velocity(0f, available.y)
        }
    }
}
