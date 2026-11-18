package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.teak.common.skeleton.TeakSkeletonBlock
import com.aritxonly.teak.common.skeleton.TeakSkeletonVideoRow
import com.aritxonly.teak.common.skeleton.rememberTeakSkeletonSweep

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.searchResultStatusItems(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors,
) {
    val results = content.results
    val users = content.users
    val loading = content.loading
    val error = content.error
    // 首个结果页的加载交给结果列表自己的骨架，这里只保留错误提示。
    error?.let { message ->
        if (results.isEmpty() && users.isEmpty()) {
            item(key = "error", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    text = message,
                    color = pageColors.onBackgroundVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }

}

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.searchVideoResultItems(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors, onOpenVideo: (VideoItem) -> Unit,
) {
    val results = content.results
    val loading = content.loading
    val loadedOnce = content.loadedOnce
    val error = content.error
    if (loading && results.isEmpty()) {
        items(count = 6, key = { "search-skeleton-$it" }) {
            SearchVideoSkeletonRow()
        }
    }
    items(items = results, key = { "r" + it.bvid }) { video ->
        val oneTake = rememberOneTakeAnchor()
        val itemContext = LocalContext.current
        val cardRadiusPx = with(LocalDensity.current) { 10.dp.toPx() }
        val open = {
                oneTake.open(itemContext, video, cardRadiusPx) {
                    onOpenVideo(video)
                }
        }
        SearchResultRow(video, open, oneTake.modifier)
    }
    if (loadedOnce && !loading && results.isEmpty() && error == null) {
        item(key = "empty-videos", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = "没有找到相关视频",
                color = pageColors.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }

}

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.searchVideoPageFooter(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors,
) {
    if (content.results.isEmpty()) return
    if (content.loadingMore) {
        item(key = "search-loading-more", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("加载中…", color = pageColors.onBackgroundVariant, fontSize = 12.sp)
            }
        }
    } else if (content.end) {
        item(key = "search-end", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("没有更多了", color = pageColors.onBackgroundVariant, fontSize = 12.sp)
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.searchUserResultItems(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors, onOpenUser: (SearchUser) -> Unit,
) {
    val users = content.users
    val loading = content.loading
    val loadedOnce = content.loadedOnce
    val error = content.error
    if (loading && users.isEmpty()) {
        items(count = 5, key = { "search-user-skeleton-$it" }) {
            SearchUserSkeletonRow()
        }
    }
    items(items = users, key = { "u" + it.mid }) { user ->
        SearchUserRow(user = user, onClick = { onOpenUser(user) })
    }
    if (loadedOnce && !loading && users.isEmpty() && error == null) {
        item(key = "empty-users", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = "没有找到相关用户",
                color = pageColors.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

/** 搜索结果首屏：和 SearchResultRow 同尺寸的骨架行。 */
@androidx.compose.runtime.Composable
private fun SearchVideoSkeletonRow() {
    TeakSkeletonVideoRow(
        progress = rememberTeakSkeletonSweep(),
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

/** 用户结果首屏：头像 + 两行文字。 */
@androidx.compose.runtime.Composable
private fun SearchUserSkeletonRow() {
    val sweep = rememberTeakSkeletonSweep()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeakSkeletonBlock(sweep, Modifier.size(44.dp), CircleShape)
        Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            TeakSkeletonBlock(sweep, Modifier.width(120.dp).height(14.dp))
            Spacer(Modifier.height(7.dp))
            TeakSkeletonBlock(sweep, Modifier.fillMaxWidth(0.6f).height(11.dp))
        }
    }
}
