package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch

/** 页面拥有状态写入，模块保留原tab布局与TeakUI组件。 */
@Composable
internal fun androidx.compose.foundation.layout.BoxScope.VideoActivityTabs(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
    pager: androidx.compose.foundation.pager.PagerState, scope: kotlinx.coroutines.CoroutineScope,
) {
    val page = LocalVideoPageState.current
    val settings = page.settings
    VideoPageTabs(layout, { page.playerBottomPx }, pager,
        onSelect = { scope.launch { pager.animateScrollToPage(it) } },
        danmakuOn = settings.danmakuOn,
        onOpenInput = { ui.danmakuInputOpen.value = true },
        onToggleDanmaku = {
            settings.danmakuOn = !settings.danmakuOn
            page.danmakuOn = settings.danmakuOn
        },
        commentCount = ui.commentCount.value,
    )
}
