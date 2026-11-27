package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.feature.feed.presentation.FeedState

/** 与空间页/直播分区一致的 320ms 滑入时值。 */
private const val FeedUpSwitchDurationMs = 320

/**
 * UP 模式在头像之间切换时的整页内容过渡：和空间页段的切换同一个观感——头像行不动，
 * 下面的动态从切换方向滑入并淡入。
 *
 * 新列表是异步拉回来的，所以方向在 filterMid 变化时就确定，但动画要等第一张卡
 * 真正到齐才开始；否则 320ms 会在骨架屏上白白播完，用户看到的还是硬切。
 */
@Composable
internal fun rememberFeedUpContentSwitch(state: FeedState, selfMid: Long?): ContentSwitchState {
    val switch = remember { ContentSwitchState() }
    // 作者/直播列表晚到会重建顺序，读最新值即可，不因此重播动画。
    val latestOrder by rememberUpdatedState(feedUpMids(state, selfMid))
    var lastMid by remember { mutableStateOf(state.filterMid) }
    var pending by remember { mutableStateOf(false) }
    var pendingDirection by remember { mutableIntStateOf(0) }
    val firstId = state.items.firstOrNull()?.id
    LaunchedEffect(state.filterMid, firstId) {
        val current = state.filterMid
        if (lastMid != current) {
            pendingDirection = feedUpSwitchDirection(lastMid, current, latestOrder)
            lastMid = current
            pending = true
        }
        if (!pending || firstId == null) return@LaunchedEffect
        pending = false
        switch.restart(pendingDirection)
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(FeedUpSwitchDurationMs, easing = LinearOutSlowInEasing),
        ) { value, _ -> switch.progress = value }
    }
    return switch
}
