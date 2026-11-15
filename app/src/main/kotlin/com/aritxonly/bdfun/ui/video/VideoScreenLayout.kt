package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.pager.PagerState
import kotlinx.coroutines.CoroutineScope
import com.aritxonly.bdfun.BrandPink

@Composable
internal fun VideoScreenLayout(
    request: VideoScreenRequest, presentation: VideoScreenPresentation,
    events: VideoScreenEvents, pager: PagerState, inlineLayout: VideoInlineLayout,
    holders: VideoScreenStateHolders, content: VideoScreenContent,
    actions: VideoDetailViewActions, relatedEnter: Float, scope: CoroutineScope,
) {
    val background = LocalVideoPageState.current.dependencies.background
    VideoDetailLayout(presentation, inlineLayout, holders.ui, content.read.loading,
        VideoDetailLayoutSlots(
            pager = { VideoScreenPager(request, presentation, pager, inlineLayout, holders.ui,
                holders.description, content, actions, relatedEnter) },
            player = { viewport -> VideoDetailPlayerWindow(content, presentation, events, inlineLayout,
                viewport, holders.ui, actions, holders.metadata, scope) },
            loading = { modifier, color -> TeakLoading(modifier = modifier, color = color) },
            togglePlayback = background::togglePlayPause, accentColor = BrandPink, playIcon = TeakIcon.PlayerPlay,
            onBack = { actions.leaveVideo() },
        ))
}
