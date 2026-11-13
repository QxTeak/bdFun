package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun FeedTopBar(
    state: FeedState,
    loggedIn: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val statusTop = rememberStatusBarTop()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FeedTopBarHeight + statusTop),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .teakBarBlur(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 8.dp),
        ) {
            TeakSegmentedBar(
                options = listOf("全部", "视频", "番剧"),
                selected = listOf("all", "video", "pgc").indexOf(state.type).coerceAtLeast(0),
                itemWidth = 64.dp,
                materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                onSelect = { index ->
                    scope.launch {
                        state.switchTo(listOf("all", "video", "pgc")[index], loggedIn)
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/**
 * UP 头像卡片：不再固定在顶栏，而是列表第一项，跟着动态一起滚走。
 * 单个圆角矩形卡片内放「全部动态 + 关注 UP 头像」的横向滚动条。
 */
