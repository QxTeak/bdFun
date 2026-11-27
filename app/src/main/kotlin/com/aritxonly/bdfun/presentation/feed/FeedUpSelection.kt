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
 * 把「换 UP」统一成一次整页推动。任何入口都先跑 [FeedUpPageTransition]，
 * 只有上一次推动还没播完时才退化为直接换页，避免吞点击。
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
        // 统一收口：先淡入/推动，动画起不来就直接换页。
        suspend fun switchTo(target: Long?) {
            val current = state.filterMid
            if (target == null) {
                val animated = transition.fadeSwap {
                    state.listState.scrollToItem(0)
                    state.clearFilter()
                }
                if (!animated) {
                    state.listState.scrollToItem(0)
                    state.clearFilter()
                }
                return
            }
            if (target == current) {
                state.listState.scrollToItem(0)
                state.filterTo(target)
                return
            }
            val direction = feedUpSwitchDirection(current, target, feedUpMids(state, latestSelfMid))
            val animated = if (direction == 0) {
                transition.fadeSwap { state.filterTo(target) }
            } else {
                transition.settle(direction) {
                    state.listState.scrollToItem(0)
                    state.filterTo(target)
                }
            }
            if (!animated) {
                state.listState.scrollToItem(0)
                state.filterTo(target)
            }
        }
        FeedUpSelection(
            selectUp = { mid -> scope.launch { switchTo(mid) } },
            exitUp = { scope.launch { switchTo(null) } },
            exitToTab = {
                scope.launch {
                    val animated = transition.fadeSwap {
                        state.exitUpFocus(latestTabKey, latestLoggedIn)
                    }
                    if (!animated) state.exitUpFocus(latestTabKey, latestLoggedIn)
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
