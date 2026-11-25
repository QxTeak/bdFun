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
        // 长按与省略号共用同一份菜单内容和选中视频；省略号额外走玻璃形变动画。
        val openMenu: (androidx.compose.ui.geometry.Rect) -> Unit = { anchor ->
            ui.actionAnchor = anchor
            ui.actionVideo = video
            ui.moreViaLongPress = true
        }
        host.card(
            video, largeCards,
            { state.open(video); onOpenVideo(video) },
            openMenu,
            openMenu,
            ui.menuState,
            {
                ui.actionVideo = video
                ui.moreViaLongPress = false
            },
        )
    }

}
