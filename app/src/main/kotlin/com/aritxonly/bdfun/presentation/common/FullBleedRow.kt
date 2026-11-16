package com.aritxonly.bdfun.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 页面横向卡片行：首卡保持页面左边距，视口向左右扩到屏幕边缘。
 *
 * 默认位置和竖向列表一致（左侧留 [sidePadding]），但右侧不受网格 padding 影响：
 * 最后一张卡被屏幕边缘自然裁切；左右滑动时也是被屏幕边缘裁切。
 */
@Composable
internal fun FullBleedRow(
    sidePadding: Dp,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalPadding: Dp = 0.dp,
    content: LazyListScope.() -> Unit,
) {
    BoxWithConstraints(modifier) {
        LazyRow(
            modifier = Modifier
                .offset(x = -sidePadding)
                .requiredWidth(maxWidth + sidePadding * 2),
            horizontalArrangement = horizontalArrangement,
            contentPadding = PaddingValues(
                start = sidePadding * 2,
                top = verticalPadding,
                bottom = verticalPadding,
            ),
        ) {
            content()
        }
    }
}
