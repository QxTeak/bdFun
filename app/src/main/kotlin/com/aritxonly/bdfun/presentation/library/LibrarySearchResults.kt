package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.library.application.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LibrarySearchResults(content: LibrarySearchContent, actions: LibrarySearchActions) {
    val colors = MiuixTheme.colorScheme
    val results = content.results; val loading = content.loading; val error = content.error; val onOpenVideo = actions.onOpenVideo
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        LazyVerticalGrid(
            columns = if (wide) BoundedVideoGridCells(RecommendedCardMinWidth.dp, RecommendedCardMaxWidth.dp) else GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 6.dp,
                bottom = 32.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading && results.isEmpty()) {
                items(count = 6, key = { "library-search-skeleton-$it" }) {
                    com.aritxonly.teak.common.skeleton.TeakSkeletonVideoCard(
                        progress = com.aritxonly.teak.common.skeleton.rememberTeakSkeletonSweep(),
                    )
                }
            }
            items(items = results, key = { it.bvid }) { video ->
                VideoGridCard(video = video, onClick = { onOpenVideo(video) })
            }
            error?.let { message ->
                if (results.isEmpty()) {
                    item(key = "error", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
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
                item(key = "none", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
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
