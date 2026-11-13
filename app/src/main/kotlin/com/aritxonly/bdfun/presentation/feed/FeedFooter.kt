package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.dynamicFeedFooter(
    state: FeedState, actions: FeedActions,
) {
    if (state.items.isEmpty() && !state.refreshing && !state.loading && state.error == null) {
        item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
            val colors = MiuixTheme.colorScheme
            Text(
                if (state.type == "up" && state.filterMid == null) "选择一位 UP 主查看动态" else "暂无动态",
                color = colors.onBackgroundVariant,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        }
    }
    if (state.hasMore && state.items.isNotEmpty() && !state.activeLoading && !state.refreshing && state.error == null) {
        item(key = "more", span = StaggeredGridItemSpan.FullLine) {
            val colors = MiuixTheme.colorScheme
            Text("加载更多", color = colors.primary, modifier = Modifier
                .fillMaxWidth().clickable { actions.loadMore() }.padding(16.dp))
        }
    }
    state.error?.let { message ->
        item(key = "error", span = StaggeredGridItemSpan.FullLine) {
            val colors = MiuixTheme.colorScheme
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message,
                    color = colors.onBackgroundVariant,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "点击重试",
                    color = colors.primary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(Capsule())
                        .clickable { actions.retry() }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
    if (state.activeLoading) {
        item(key = "loading", span = StaggeredGridItemSpan.FullLine) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                TeakLoading(size = 34.dp)
            }
        }
    }
}
