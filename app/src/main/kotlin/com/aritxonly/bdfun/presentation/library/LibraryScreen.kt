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
internal fun LibraryScreen(
    request: LibraryScreenRequest, navigation: LibraryNavigation, dependencies: LibraryDependencies,
    searchOverlayHost: LibrarySearchOverlayHost? = null,
) {
    val runtime = rememberLibraryRuntime(request, navigation, dependencies)
    LibraryEffects(runtime)
    if (!request.loggedIn) { LibraryGuest(navigation.onNeedLogin); return }
    LibraryContent(runtime, searchOverlayHost)
}

@Composable
private fun LibraryContent(runtime: LibraryRuntime, searchOverlayHost: LibrarySearchOverlayHost? = null) {
    val data by androidx.compose.runtime.key(runtime.data) { runtime.data.state.collectAsState() }
    val interaction by androidx.compose.runtime.key(runtime.interaction) { runtime.interaction.state.collectAsState() }
    val pagerState = runtime.pager
    val progress = remember { derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction } }
    if (searchOverlayHost != null) {
        // 搜索页挂在 Activity 的 overlay 槽里：这里只交付运行时和展开状态，
        // 数据由搜索页自己 collect，避免闭包残留。
        androidx.compose.runtime.SideEffect {
            searchOverlayHost.runtime = runtime
            searchOverlayHost.searching = interaction.searching
        }
        androidx.compose.runtime.DisposableEffect(searchOverlayHost) {
            onDispose {
                searchOverlayHost.searching = false
                searchOverlayHost.runtime = null
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalViewConfiguration provides rememberStricterSwipeConfiguration()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0, overscrollEffect = null, pageSpacing = 12.dp) { page ->
                LibraryPagerPage(runtime, data, interaction, progress, runtime.request.fixedMode ?: page)
            }
        }

        LibraryTransferSheet(runtime, data, interaction)
        // 长按视频：和首页同款底部操作面板，选项复用批量管理。
        LibraryVideoMenu(
            show = runtime.presentation.sheetVideo != null,
            anchor = runtime.presentation.sheetAnchor,
            allowTransfer = runtime.currentMode == 0,
            onDismiss = runtime::dismissSheet,
            onAction = { kind -> runtime.presentation.sheetVideo?.let { runtime.sheetAction(kind, it) } },
        )
    }
}

@Composable
internal fun LibraryPagerPage(
    runtime: LibraryRuntime, data: LibraryUiState, interaction: LibraryInteractionState, progress: State<Float>, mode: Int,
) {
    LibraryModePage(
        content = libraryModeContent(runtime.interaction, data, interaction, mode),
        layout = LibraryModeLayout(mode, runtime.request.title, runtime.request.fixedMode == null,
            mode == runtime.currentMode, progress, runtime.presentation.scrollTopTick),
        actions = LibraryModeActions(runtime.data, runtime::selectMode, runtime.interaction::toggleSelected,
            runtime::selectFolder, runtime.navigation.onOpenVideo, { runtime.loadMore(mode) },
            runtime.dependencies.playbackQueue::set,
            onVideoMenu = runtime::openSheet,
            onPrepareCollection = { items, bvid, title ->
                runtime.dependencies.playbackQueue.set(items, bvid, title)
                runtime.dependencies.localCollection.set(bvid, items, title)
            },
        ),
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
    allWatchLater = data.watchLater.map { it.toVideoItem() },
)

@Composable
internal fun LibrarySearchOverlay(
    runtime: LibraryRuntime, data: LibraryUiState, ui: LibraryInteractionState,
    windowAlpha: Float, maskAlpha: Float, contentAlpha: Float, rise: Float,
) {
    val interaction = runtime.interaction
    val mode = runtime.currentMode
    // 搜索页在动画期间每帧都会重组：按数据/关键词缓存过滤结果，
    // 避免每帧对整页收藏/历史做一遍 filter + map。
    val results = remember(
        mode, ui.query, ui.watchLaterVisibleCount,
        data.favorites.videos, data.history.entries, data.watchLater,
    ) {
        when (mode) {
            0 -> interaction.visibleVideos(data.favorites.videos)
            1 -> interaction.visibleEntries(data.history.entries).map { it.toVideoItem() }
            else -> interaction.visibleEntries(data.watchLater).take(ui.watchLaterVisibleCount).map { it.toVideoItem() }
        }
    }
    LibrarySearchPage(
        LibrarySearchContent(runtime.request.title, ui.searchHistory, results, data.loading[mode] == true, data.errors[mode]),
        // 搜索结果里点开的视频同样先准备队列，否则播完没有下一集可连。
        LibrarySearchActions(interaction::clearSearchHistory, interaction::submitQuery, interaction::closeSearch) { video ->
            runtime.dependencies.playbackQueue.set(results, video.bvid)
            runtime.navigation.onOpenVideo(video)
        },
        windowAlpha = windowAlpha,
        maskAlpha = maskAlpha,
        contentAlpha = contentAlpha,
        rise = rise,
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
