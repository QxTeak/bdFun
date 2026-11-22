package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.skeleton.TeakSkeletonBlock
import com.aritxonly.teak.common.skeleton.rememberTeakSkeletonSweep
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 动态首屏加载：统一改用 Miuix 加载动画（骨架卡片代码保留，不再挂到列表上）。 */
internal fun LazyStaggeredGridScope.dynamicFeedSkeletonItems() {
    item(
        key = "feed-loading",
        span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine,
    ) {
        MiuixLoadingRow()
    }
}

@Composable
private fun FeedCardSkeleton() {
    val sweep = rememberTeakSkeletonSweep()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeakSkeletonBlock(sweep, Modifier.size(34.dp), CircleShape)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                TeakSkeletonBlock(sweep, Modifier.width(96.dp).height(12.dp))
                Spacer(Modifier.height(6.dp))
                TeakSkeletonBlock(sweep, Modifier.width(64.dp).height(10.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth().height(13.dp))
        Spacer(Modifier.height(6.dp))
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth(0.86f).height(13.dp))
        Spacer(Modifier.height(6.dp))
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth(0.6f).height(13.dp))
        Spacer(Modifier.height(10.dp))
        TeakSkeletonBlock(sweep, Modifier.fillMaxWidth().aspectRatio(16f / 9f), RoundedCornerShape(10.dp))
    }
}
