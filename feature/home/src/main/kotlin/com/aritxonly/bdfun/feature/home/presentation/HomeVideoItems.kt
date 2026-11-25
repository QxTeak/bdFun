package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuState

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.homeVideoItems(
    state: HomeFeedState, largeCards: Boolean, ui: HomePresentationStateHolder,
    onOpenVideo: (VideoItem) -> Unit, host: HomePresentationHost,
) {
    items(items = state.videos, key = { it.bvid }) { video ->
        // 每张卡片独立锚点：Miuix 的 anchor 只记录最后一次 bounds。
        val cardMenu = remember(video.bvid) { TeakMorphMenuState() }
        // 长按：从手指位置长出来，不画省略号副本；省略号：从图标长出来。
        val openLongPress: (androidx.compose.ui.geometry.Rect) -> Unit = { point ->
            state.menuVideo = video
            state.menuState = ui.pointMenuState
            ui.pressPoint = androidx.compose.ui.geometry.Offset(point.left, point.top)
            ui.openPointMenu = true
        }
        host.card(
            video, largeCards,
            { state.open(video); onOpenVideo(video) },
            openLongPress,
            openLongPress,
            cardMenu,
            {
                state.menuVideo = video
                state.menuState = cardMenu
            },
        )
    }
}
