package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakGrabberBar

/**
 * UP 页顶部的下拉退出条。
 *
 * 视觉与 Sheet 拖拽条完全同源：45dp × 4dp，按压缩到 55dp / 1.15 倍。
 * 只有这根条接手势，列表本身不用下拉刷新，也不会和滚动抢事件。
 */
@Composable
internal fun FeedUpExitGrabber(
    pull: FeedUpPullController,
    modifier: Modifier = Modifier,
) {
    val dragState = rememberDraggableState { delta -> pull.drag(delta) }
    val pressed = pull.pressed
    val width by animateDpAsState(
        targetValue = if (pressed) 55.dp else 45.dp,
        animationSpec = tween(if (pressed) 100 else 150),
        label = "feedUpGrabberWidth",
    )
    Box(
        modifier = modifier.draggable(
            state = dragState,
            orientation = Orientation.Vertical,
            onDragStarted = { pull.start() },
            onDragStopped = { velocity -> pull.end(velocity) },
        ),
        contentAlignment = Alignment.Center,
    ) {
        TeakGrabberBar(width = width, pressed = pressed)
    }
}
