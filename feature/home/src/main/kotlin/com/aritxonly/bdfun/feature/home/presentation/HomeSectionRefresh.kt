package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 首页三个分段（推荐 / 直播 / 番剧）共用的「再次点击底栏当前页 → 回顶 + 下拉 + 刷新」编排。
 *
 * 三处之前各写了一份同样的时序；现在只保留这一份，调用方只提供自己的
 * 网格、下拉补间、忙碌判断和刷新动作。用「已处理到第几号请求」做幂等，
 * 页面被销毁重建时不会重放刷新。
 */
@Composable
fun HomeSectionRefreshEffect(
    request: Int,
    gridState: LazyGridState,
    pullThresholdPx: Float,
    animatePull: suspend (targetPx: Float, durationMs: Float) -> Unit,
    isBusy: () -> Boolean,
    onPulling: (fraction: Float) -> Unit,
    refresh: suspend () -> Unit,
) {
    var handled by remember { mutableIntStateOf(request) }
    LaunchedEffect(request) {
        if (request == handled) return@LaunchedEffect
        handled = request
        if (isBusy()) return@LaunchedEffect
        val atTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        if (atTop) {
            // 已经在顶部：只拉下拉指示器，200ms 收放一次。
            animatePull(pullThresholdPx, 200f)
        } else {
            // 回顶和下拉并行，衔接处不停顿（时序与推荐页原实现一致）。
            coroutineScope {
                launch { gridState.animateScrollToItem(0) }
                launch { animatePull(pullThresholdPx, 260f) }
            }
        }
        onPulling(1f)
        refresh()
    }
}
