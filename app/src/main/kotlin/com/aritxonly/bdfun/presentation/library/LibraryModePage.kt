package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.library.application.*
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LibraryModePage(content: LibraryModeContent, layout: LibraryModeLayout, actions: LibraryModeActions) {
    val gridState = rememberLazyGridState()
    RegisterScrollTop(gridState, "library")
    if (layout.isActive) TrackTeakPageScroll(gridState)
    LibraryModeEffects(gridState, layout, actions)
    Box(modifier = Modifier.fillMaxSize()) {
        LibraryGrid(content, layout, actions, gridState)
        if (content.loading) TeakLoading(modifier = Modifier.align(Alignment.Center), color = MiuixTheme.colorScheme.primary, size = 48.dp)
    }
}

@Composable
private fun LibraryGrid(content: LibraryModeContent, layout: LibraryModeLayout, actions: LibraryModeActions,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyVerticalGrid(
        state = gridState, columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = TeakChromeDefaults.ContentHorizontalPadding,
            end = TeakChromeDefaults.ContentHorizontalPadding, top = statusBarTop + TeakChromeDefaults.ContentTopPadding,
            bottom = if (content.batchMode) 92.dp else TeakChromeDefaults.ContentBottomPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        libraryGridHeader(content, layout, actions)
        if (layout.mode == 0) {
            itemsIndexed(items = content.folderVideos, key = { _, video -> video.bvid }) { index, video ->
                LibraryFavoriteCard(video, index, content, layout, actions)
            }
        } else {
            val entries = if (layout.mode == 1) content.historyEntries else content.watchLaterEntries
            itemsIndexed(items = entries, key = { _, entry -> entry.bvid }) { index, entry ->
                LibraryHistoryCard(entry, index, content, layout, actions)
            }
        }
        libraryGridFooter(content, layout, actions)
    }
}

@Composable
internal fun LibraryModeEffects(
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState, layout: LibraryModeLayout, actions: LibraryModeActions,
) {
    if (layout.isActive) {
        LaunchedEffect(gridState, actions.owner) {
            snapshotFlow { gridState.layoutInfo.totalItemsCount to gridState.canScrollForward }.collect { (total, canScroll) ->
                if (total > 0 && !canScroll) actions.onLoadMore()
            }
        }
    }
    val ui = remember { LibraryGridPresentationStateHolder(layout.scrollTopTick) }
    LaunchedEffect(layout.scrollTopTick, layout.isActive) {
        if (!layout.isActive) { ui.seenScrollTop = layout.scrollTopTick; return@LaunchedEffect }
        if (layout.scrollTopTick == ui.seenScrollTop) return@LaunchedEffect
        ui.seenScrollTop = layout.scrollTopTick
        if (layout.scrollTopTick > 0) gridState.animateScrollToItem(0)
    }
}

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.libraryGridHeader(
    content: LibraryModeContent, layout: LibraryModeLayout, actions: LibraryModeActions,
) {
    val title = layout.title; val mode = layout.mode; val showModeTabs = layout.showModeTabs
    val modeProgress = layout.modeProgress; val folders = content.folders; val foldersReady = content.foldersReady
    val folderIndex = content.folderIndex; val onSelectMode = actions.onSelectMode; val onFolderSelect = actions.onFolderSelect
            item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
                TeakLargeTitle(
                    title = title,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            if (showModeTabs) {
                item(key = "modes", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TeakPagerTabs(
                            options = MODES,
                            selected = mode,
                            onSelect = onSelectMode,
                            itemWidth = 96.dp,
                            backdrop = null,
                            plain = true,
                            position = modeProgress,
                        )
                    }
                }
            }

            if (mode == 0 && folders.isNotEmpty() && foldersReady) {
                item(key = "folders", span = { GridItemSpan(maxLineSpan) }) {
                    // 首次加载完成后再出现；之后切换收藏夹、重载列表时保持原位，不重播动画。
                    LibraryFolderBar(
                        folders = folders,
                        selectedIndex = folderIndex,
                        onSelect = onFolderSelect,
                    )
                }
            }


}

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.libraryGridFooter(
    content: LibraryModeContent, layout: LibraryModeLayout, actions: LibraryModeActions,
) {
    val mode = layout.mode; val canLoadMore = content.canLoadMore
    val folderVideos = content.folderVideos; val historyEntries = content.historyEntries; val watchLaterEntries = content.watchLaterEntries
    val error = content.error; val onLoadMore = actions.onLoadMore
            if (canLoadMore) {
                item(key = "load-more", span = { GridItemSpan(maxLineSpan) }) {
                    // 滑到底时这个哨兵会被组合，随即请求下一页；用列表长度做 key，
                    // 新数据到达后如果还没到底，会再次触发。
                    val dataSize = if (mode == 0) folderVideos.size else if (mode == 1) {
                        historyEntries.size
                    } else {
                        watchLaterEntries.size
                    }
                    LaunchedEffect(actions.owner, dataSize) {
                        com.aritxonly.bdfun.data.LogCollector.record(
                            "HISTORY",
                            "bottom sentinel mode=$mode size=$dataSize canLoadMore=$canLoadMore",
                        )
                        onLoadMore()
                    }
                    Spacer(Modifier.height(1.dp))
                }
            }

            error?.let { message ->
                item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(vertical = 20.dp),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = 13.sp,
                    )
                }
            }
}
