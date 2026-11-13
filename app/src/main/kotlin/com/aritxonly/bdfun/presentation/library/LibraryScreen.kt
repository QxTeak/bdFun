package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.library.application.*

@Composable
internal fun LibraryScreen(request: LibraryScreenRequest, navigation: LibraryNavigation, dependencies: LibraryDependencies) {
    val runtime = rememberLibraryRuntime(request, navigation, dependencies)
    LibraryEffects(runtime)
    if (!request.loggedIn) { LibraryGuest(navigation.onNeedLogin); return }
    LibraryContent(runtime)
}

@Composable
private fun LibraryContent(runtime: LibraryRuntime) {
    val data by androidx.compose.runtime.key(runtime.data) { runtime.data.state.collectAsState() }
    val interaction by androidx.compose.runtime.key(runtime.interaction) { runtime.interaction.state.collectAsState() }
    val pagerState = runtime.pager
    val progress = remember { derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction } }
    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalViewConfiguration provides rememberStricterSwipeConfiguration()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0, overscrollEffect = null, pageSpacing = 12.dp) { page ->
                LibraryPagerPage(runtime, data, interaction, progress, runtime.request.fixedMode ?: page)
            }
        }
        if (interaction.searching) LibrarySearchOverlay(runtime, data, interaction)
        LibraryTransferSheet(runtime, data, interaction)
    }
}

@Composable
internal fun LibraryPagerPage(
    runtime: LibraryRuntime, data: LibraryUiState, interaction: LibraryInteractionState, progress: State<Float>, mode: Int,
) {
    LibraryModePage(
        content = libraryModeContent(runtime.interaction, data, interaction, mode),
        layout = LibraryModeLayout(mode, runtime.request.title, runtime.request.fixedMode == null,
            mode == runtime.currentMode, progress, runtime.presentation.scrollTopTick, runtime.dependencies.entrance),
        actions = LibraryModeActions(runtime.data, runtime::selectMode, runtime.interaction::toggleSelected,
            runtime::selectFolder, runtime.navigation.onOpenVideo, { runtime.loadMore(mode) }, runtime.dependencies.playbackQueue::set),
    )
}
internal fun libraryModeContent(
    interaction: LibraryInteractionStateHolder, data: LibraryUiState, ui: LibraryInteractionState, mode: Int,
) = LibraryModeContent(
    data.favorites.folders, data.favorites.ready, data.favorites.index,
    interaction.visibleVideos(data.favorites.videos), interaction.visibleEntries(data.history.entries),
    interaction.visibleEntries(data.watchLater).take(ui.watchLaterVisibleCount), data.loading[mode] == true,
    data.errors[mode], ui.batchMode, ui.selected,
    when(mode) { 0 -> data.favorites.hasMore; 1 -> data.history.hasMore; else -> ui.watchLaterVisibleCount < data.watchLater.size },
)

@Composable
internal fun LibrarySearchOverlay(runtime: LibraryRuntime, data: LibraryUiState, ui: LibraryInteractionState) {
    val interaction = runtime.interaction
    val results = when(runtime.currentMode) {
        0 -> interaction.visibleVideos(data.favorites.videos)
        1 -> interaction.visibleEntries(data.history.entries).map { it.toVideoItem() }
        else -> interaction.visibleEntries(data.watchLater).take(ui.watchLaterVisibleCount).map { it.toVideoItem() }
    }
    LibrarySearchPage(
        LibrarySearchContent(runtime.request.title, ui.searchHistory, results, data.loading[runtime.currentMode] == true, data.errors[runtime.currentMode]),
        LibrarySearchActions(interaction::clearSearchHistory, interaction::submitQuery, interaction::closeSearch, runtime.navigation.onOpenVideo),
    )
}

@Composable
internal fun LibraryTransferSheet(runtime: LibraryRuntime, data: LibraryUiState, ui: LibraryInteractionState) {
    val folders = data.favorites.folders
    val folderIndex = data.favorites.index
    // 移动 / 复制：与视频页点「收藏」后弹出的收藏夹菜单同一套底部面板。
    val activeTransfer = ui.transferMode
    val sourceId = folders.getOrNull(folderIndex)?.id
    val targets = folders.filter { it.id != sourceId }
    com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheet(
        show = activeTransfer != null,
        state = com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheetUiState(
            title = if (activeTransfer == 1) "移动到收藏夹" else "复制到收藏夹",
            description = "选择目标收藏夹 · 已选择 ${ui.selected.size} 个视频",
            folders = targets.map { folder ->
                com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderOption(
                    folder.id, folder.title, folder.count,
                )
            },
            emptyMessage = "没有其他可用的收藏夹",
        ),
        onDismiss = runtime.interaction::dismissTransfer,
        onSelect = { id -> targets.firstOrNull { it.id == id }?.let { runtime.transfer(activeTransfer ?: 2, it) } },
    )
}
