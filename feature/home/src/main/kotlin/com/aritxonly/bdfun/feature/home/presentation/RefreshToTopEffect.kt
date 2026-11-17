package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 推荐 / 直播 / 番剧 / 动态共用的「再次点击底栏当前页 → 回顶 + 下拉 + 刷新」编排。
 *
 * 四个页面之前各写了一份同样的时序；现在只保留这一份，调用方只提供自己的
 * 下拉补间、忙碌判断和滚动几何（网格和普通列表都用 LazyListState 的
 * firstVisibleItemIndex / Offset，所以这里不绑定具体滚动容器类型）。
 *
 * 用「已处理到第几号请求」做幂等，页面被销毁重建时不会重放刷新。
 */
@Composable
fun RefreshToTopEffect(
    request: Int,
    pullThresholdPx: Float,
    animatePull: suspend (targetPx: Float, durationMs: Float) -> Unit,
    isBusy: () -> Boolean,
    onPulling: (fraction: Float) -> Unit,
    isAtTop: () -> Boolean,
    scrollToTop: suspend () -> Unit,
    refresh: suspend () -> Unit,
) {
    var handled by remember { mutableIntStateOf(request) }
    LaunchedEffect(request) {
        if (request == handled) return@LaunchedEffect
        handled = request
        if (isBusy()) return@LaunchedEffect
        if (isAtTop()) {
            // 已经在顶部：只拉下拉指示器，200ms 收放一次。
            animatePull(pullThresholdPx, 200f)
        } else {
            // 回顶和下拉并行，衔接处不停顿（时序与推荐页原实现一致）。
            coroutineScope {
                launch { scrollToTop() }
                launch { animatePull(pullThresholdPx, 260f) }
            }
        }
        onPulling(1f)
        refresh()
    }
}
