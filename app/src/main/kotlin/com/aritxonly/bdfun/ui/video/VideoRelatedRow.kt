package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.data.VideoItem

/**
 * 相关推荐 / 空间投稿共用的视频行。
 *
 * 直接复用搜索结果页的 [SearchResultRow]：同一套布局、同一个一镜到底起飞点，
 * 不再维护第二份行 UI——两处必然长得一样、动画也走同一条系统路径。
 */
@Composable
internal fun RelatedRow(
    item: VideoItem,
    onClick: () -> Unit,
    /** 空间投稿页传发布时间，用来顶掉底部的 UP 名字；视频页相关推荐不传。 */
    metaLabel: String? = null,
    /** 视频页由外层列表控制左右留白时为 16dp；空间投稿页已自己给过留白，传 0。 */
    horizontalPadding: Dp = 16.dp,
) {
    val context = LocalContext.current
    val oneTake = rememberOneTakeAnchor()
    // 与搜索结果行保持一致的圆角基准。
    val radiusPx = with(LocalDensity.current) { 10.dp.toPx() }
    Box(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding)) {
        SearchResultRow(
            video = item,
            onClick = { oneTake.open(context, item, radiusPx) { onClick() } },
            modifier = oneTake.modifier,
            // 相关推荐/投稿列表不显示发布时间行里的日期：前者直接不要，后者挪到 UP 名字的位置。
            showDate = false,
            metaLabel = metaLabel,
        )
    }
}
