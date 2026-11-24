package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.library.application.*
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索结果：和媒体库默认页完全同一套视频行与宽屏有界网格，不再单独维护
 * 一份 2 列封面卡，这样「搜到的东西」看起来就是本页原本的列表。
 */
@Composable
internal fun LibrarySearchResults(
    content: LibrarySearchContent, actions: LibrarySearchActions, topInset: Dp,
) {
    val colors = MiuixTheme.colorScheme
    val results = content.results
    val loading = content.loading
    val error = content.error
    val gridState = rememberLazyGridState()
    // 内容滚到搜索栏下面时，搜索栏的渐进模糊才出现。
    TrackTeakPageScroll(gridState)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        LazyVerticalGrid(
            state = gridState,
            columns = if (wide) BoundedVideoGridCells(280.dp, 420.dp) else GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = topInset,
                bottom = TeakChromeDefaults.ContentBottomPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (loading && results.isEmpty()) {
                videoResultSkeleton(keyPrefix = "library-search-skeleton")
            }
            videoResultRows(
                videos = results,
                keyPrefix = "library-search-",
                onOpenVideo = actions.onOpenVideo,
            )
            error?.let { message ->
                if (results.isEmpty()) {
                    item(key = "library-search-error", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = message,
                            color = colors.onBackgroundVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        )
                    }
                }
            }
            if (!loading && results.isEmpty() && error == null) {
                item(key = "library-search-empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "没有找到相关内容",
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    )
                }
            }
        }
    }
}
