package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.homeVideoItems(
    state: HomeFeedState, largeCards: Boolean, ui: HomePresentationStateHolder,
    onOpenVideo: (VideoItem) -> Unit, host: HomePresentationHost,
) {
    items(items = state.videos, key = { it.bvid }) { video ->
        // 每张卡片独立锚点；打开时把该卡片的锚点交给页面唯一的菜单宿主。
        val cardMenu = androidx.compose.runtime.remember(video.bvid) {
            com.aritxonly.teak.common.navigation.TeakMorphMenuState()
        }
        // 长按与省略号走同一套形变；长按不画省略号副本，点击省略号时画。
        val openMenu: (androidx.compose.ui.geometry.Rect) -> Unit = {
            state.menuVideo = video
            state.menuState = cardMenu
            cardMenu.openWithAnchorCopy(false)
        }
        host.card(
            video, largeCards,
            { state.open(video); onOpenVideo(video) },
            openMenu,
            openMenu,
            cardMenu,
            {
                state.menuVideo = video
                state.menuState = cardMenu
                state.menuViaLongPress = false
            },
        )
    }

}
