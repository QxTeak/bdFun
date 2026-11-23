package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

@Composable
internal fun BoxScope.VideoActivityOverlay(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
    pager: androidx.compose.foundation.pager.PagerState, scope: kotlinx.coroutines.CoroutineScope,
    contentBackdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop? = null,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        com.aritxonly.teak.common.theme.LocalTeakControlEmphasis provides true,
    ) {
    VideoPageOverlay(layout, ui, pager, LocalVideoPageState.current, VideoPageOverlaySlots(
        tabs = {
            // Tab 和弹幕控件只采播放器下方的内容底片，整页底片仍留给返回键等其它玻璃。
            androidx.compose.runtime.CompositionLocalProvider(
                com.aritxonly.teak.common.material.LocalTeakBackdrop provides contentBackdrop,
            ) { VideoActivityTabs(layout, ui, pager, scope) }
        },
        replyBar = { modifier -> CommentReplyBar(modifier = modifier) },
        inputBar = { onDismiss, modifier -> DanmakuInputBar(onDismiss = onDismiss, modifier = modifier) },
    ))
    }
}
