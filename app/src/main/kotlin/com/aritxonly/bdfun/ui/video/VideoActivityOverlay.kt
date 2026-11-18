package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

@Composable
internal fun BoxScope.VideoActivityOverlay(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
    pager: androidx.compose.foundation.pager.PagerState, scope: kotlinx.coroutines.CoroutineScope,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        com.aritxonly.teak.common.theme.LocalTeakControlEmphasis provides true,
    ) {
    VideoPageOverlay(layout, ui, pager, LocalVideoPageState.current, VideoPageOverlaySlots(
        tabs = { VideoActivityTabs(layout, ui, pager, scope) },
        replyBar = { modifier -> CommentReplyBar(modifier = modifier) },
        inputBar = { onDismiss, modifier -> DanmakuInputBar(onDismiss = onDismiss, modifier = modifier) },
        panel = { onInteract, backdrop, modifier ->
            DanmakuSettingsPanel(onInteract = onInteract, backdrop = backdrop,
                settingsContent = { interact ->
                    DanmakuSettingsContent(fullscreen = false, onInteract = interact) { DanmakuCloudRulesEntry(interact) }
                },
                modifier = modifier)
        },
    ))
    }
}
