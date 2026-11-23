package com.aritxonly.bdfun.ui


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.interaction.blockTouches
import com.aritxonly.teak.common.chrome.rememberManualProgress

@Composable
fun androidx.compose.foundation.layout.BoxScope.VideoPageOverlay(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
    pager: androidx.compose.foundation.pager.PagerState, host: VideoPageOverlayHost, slots: VideoPageOverlaySlots,
) {
    if (layout.pictureInPicture) return
    val page = host
    LaunchedEffect(layout.fullscreen) {
        if (layout.fullscreen) {
            page.dismissReply++
            ui.danmakuInputOpen.value = false
        }
    }
    if (layout.overlayAlpha > 0.01f) {
        slots.tabs(this)
        VideoActivityReplyLayer(layout, pager, host, slots)
        VideoActivityInputLayer(layout, ui, host, slots)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoActivityReplyLayer(
    layout: VideoActivityLayout, pager: androidx.compose.foundation.pager.PagerState,
    host: VideoPageOverlayHost, slots: VideoPageOverlaySlots,
) {
    val VideoPageBus = host
    val PlayerUiState = host
    val pipMode = layout.pictureInPicture
    val pageFullscreen = layout.fullscreen
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    val playerTopInset = layout.playerTopInset
    val fs = pageFullscreen
    // 评论回复栏属于详情栏；只有独占全屏才隐藏。
    if (!fs && pager.currentPage == 1) {
        // 编辑中：铺一层全屏点击层，点空白处直接退出（同时杜绝点击穿透到播放器）
        if (VideoPageBus.replyEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val down = event.changes.firstOrNull { it.pressed }
                                if (down != null) {
                                    VideoPageBus.dismissReply++
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
            )
        }
        // 长按评论弹出的下拉是独立 Dialog 窗口，层级本来就在输入栏之上；
        // 不能再因为菜单打开把人输入栏淡出，否则用户会以为输入框被吞了。
        slots.replyBar(
            Modifier
                .align(if (splitPanes) Alignment.BottomEnd else Alignment.BottomCenter)
                .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier),
        )
    }

}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoActivityInputLayer(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
    host: VideoPageOverlayHost, slots: VideoPageOverlaySlots,
) {
    val VideoPageBus = host
    val PlayerUiState = host
    val pipMode = layout.pictureInPicture
    val pageFullscreen = layout.fullscreen
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    val playerTopInset = layout.playerTopInset
    val fs = pageFullscreen
    var danmakuInputOpen by ui.danmakuInputOpen
    // 发弹幕输入
    if (danmakuInputOpen && !fs) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val down = event.changes.firstOrNull { it.pressed }
                            if (down != null) VideoPageBus.danmakuInputClose++
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
        )
        slots.inputBar(
            { danmakuInputOpen = false },
            Modifier
                .align(if (splitPanes) Alignment.BottomStart else Alignment.BottomCenter)
                .then(if (splitPanes) Modifier.width(paneLayout.playerWidthDp.dp) else Modifier),
        )
    }
}
