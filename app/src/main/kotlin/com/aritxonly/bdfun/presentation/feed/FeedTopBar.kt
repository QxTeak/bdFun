package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.domain.DynamicFeedTab
import com.aritxonly.teak.common.chrome.teakClassicBarGlass

/**
 * 动态页固定顶栏：只画「全部 / 视频 / 番剧」玻璃分段栏。
 *
 * 选中态由 pager 所在的 shell 持有（和首页分段同一套结构），这里只负责显示与点选，
 * 不再自己 switchTo，避免顶栏和页面各改一份分类状态。
 */
@Composable
fun FeedTopBar(
    tabs: List<DynamicFeedTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** 跟手的连续位置（和首页同一套胶囊指示器）；null 时只按选中项动画。 */
    position: androidx.compose.runtime.State<Float>? = null,
) {
    val statusTop = rememberStatusBarTop()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FeedTopBarHeight + statusTop),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
                        Modifier.teakClassicBarGlass()
                    } else {
                        Modifier.teakBarBlur()
                    },
                ),
        )
        if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f)),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 8.dp),
        ) {
            TeakSegmentedBar(
                options = tabs.map { it.title },
                selected = selectedIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
                itemWidth = 64.dp,
                materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                position = position,
                onSelect = onSelect,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/**
 * UP 头像卡片：不再固定在顶栏，而是列表第一项，跟着动态一起滚走。
 * 单个圆角矩形卡片内放「全部动态 + 关注 UP 头像」的横向滚动条。
 */
