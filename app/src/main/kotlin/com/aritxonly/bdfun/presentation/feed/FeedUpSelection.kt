package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.aritxonly.bdfun.feature.feed.presentation.FeedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** UP 模式下所有入口（头像点击、左右滑、返回）共用的切页动作。 */
@Stable
internal class FeedUpSelection(
    val selectUp: (Long) -> Unit,
    /** 点「全部动态」：回到该分段自己的分类。 */
    val exitUp: () -> Unit,
    /** 返回键 / 点顶部分段：切回这一页的分类。 */
    val exitToTab: () -> Unit,
    /** 左右滑松手：+1 下一个 UP、-1 上一个、0 回弹。 */
    val onSwipeRelease: (Int) -> Unit,
)

/**
 * 把「换 UP」统一成一次左右推动。只有不同 UP 之间有方向；
 * 进入/退出 UP 模式没有左右方向，直接换页，不做整屏淡入（那会看起来像闪一下）。
 */
@Composable
internal fun rememberFeedUpSelection(
    state: FeedState,
    scope: CoroutineScope,
    transition: FeedUpPageTransition,
    selfMid: Long?,
    tabKey: String,
    loggedIn: Boolean,
): FeedUpSelection {
    val latestSelfMid by rememberUpdatedState(selfMid)
    val latestTabKey by rememberUpdatedState(tabKey)
    val latestLoggedIn by rememberUpdatedState(loggedIn)
    return remember(state, scope, transition) {
        // 换页本身：先把新页滚回顶部，再提交筛选。
        suspend fun commit(target: Long?) {
            state.listState.scrollToItem(0)
            if (target == null) state.clearFilter() else state.filterTo(target)
        }
        suspend fun switchTo(target: Long?) {
            val current = state.filterMid
            if (target == null || target == current) {
                commit(target)
                return
            }
            val direction = feedUpSwitchDirection(current, target, feedUpMids(state, latestSelfMid))
            if (direction == 0) {
                // 进入 UP 模式 / 顺序未知：不做左右推动，直接换页。
                commit(target)
                return
            }
            val animated = transition.settle(direction) { commit(target) }
            // 上一次推动还没播完时不吞点击，直接换页。
            if (!animated) commit(target)
        }
        FeedUpSelection(
            selectUp = { mid -> scope.launch { switchTo(mid) } },
            exitUp = { scope.launch { commit(null) } },
            exitToTab = {
                scope.launch {
                    state.exitUpFocus(latestTabKey, latestLoggedIn)
                }
            },
            onSwipeRelease = { delta ->
                scope.launch {
                    val current = state.filterMid
                    val order = feedUpMids(state, latestSelfMid)
                    val target = if (delta != 0 && current != null) {
                        order.getOrNull(order.indexOf(current) + delta)
                    } else {
                        null
                    }
                    if (target == null) {
                        transition.settle(0, null)
                    } else {
                        switchTo(target)
                    }
                }
            },
        )
    }
}
