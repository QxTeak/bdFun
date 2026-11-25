package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    }
}

@Composable
private fun LibraryGrid(content: LibraryModeContent, layout: LibraryModeLayout, actions: LibraryModeActions,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 和热门页/搜索结果完全同一套：一列视频行，宽屏用有界网格；
        // 骨架、分页尾巴也直接复用那两份实现，不再单独维护一套卡片。
        val wide = maxWidth >= 600.dp
        LibraryGridBody(content, layout, actions, gridState, wide, statusBarTop)
    }
}

/** 网格主体：标签状态、内容切换过渡和列表本身，拆出来是为了守住单函数行数门槛。 */
@Composable
private fun LibraryGridBody(
    content: LibraryModeContent, layout: LibraryModeLayout, actions: LibraryModeActions,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState, wide: Boolean,
    statusBarTop: androidx.compose.ui.unit.Dp,
) {
    var historyCategory by remember { mutableStateOf(HistoryCategory.All) }
    // 收藏夹/历史分类切换的滑动方向：按标签索引差给出，内容从切换那一侧滑进来。
    var switchDirection by remember { mutableStateOf(1) }
    val switch = rememberContentSwitch(
        Triple(layout.mode, content.folderIndex, historyCategory), switchDirection,
    )
    // 列表滚动时 LazyGrid 的内容 lambda 会反复执行，过滤/映射放在外面只算一次。
    val rows = remember(
        content.folderVideos, content.historyEntries, content.watchLaterEntries,
        layout.mode, historyCategory,
    ) {
        when (layout.mode) {
            0 -> content.folderVideos
            1 -> content.historyEntries
                .filter { historyCategory == HistoryCategory.All || it.historyCategory() == historyCategory }
                .map { it.toVideoItem() }
            else -> content.watchLaterEntries.map { it.toVideoItem() }
        }
    }
    // 稍后再看进视频时用完整列表建队列，历史/收藏仍按当前可见结果。
    val queueRows = if (layout.mode == 2 && content.allWatchLater.isNotEmpty()) content.allWatchLater else rows
    androidx.compose.runtime.CompositionLocalProvider(LocalContentSwitch provides switch) {
        // 组合期取好：LazyGrid 的 scope 不是 @Composable，不能在里面读 CompositionLocal。
        val switchItem = Modifier.contentSwitchItem()
        LazyVerticalGrid(
            state = gridState,
            columns = if (wide) BoundedVideoGridCells(280.dp, 420.dp) else GridCells.Fixed(1),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp,
                top = statusBarTop + TeakChromeDefaults.ContentTopPadding,
                bottom = if (content.batchMode) 92.dp else TeakChromeDefaults.ContentBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            libraryGridHeader(content, layout, actions, historyCategory,
                onHistoryCategory = { category ->
                    val from = HistoryCategory.entries.indexOf(historyCategory)
                    val to = HistoryCategory.entries.indexOf(category)
                    switchDirection = if (to >= from) 1 else -1
                    historyCategory = category
                },
                onFolderSelect = { index ->
                    switchDirection = if (index >= content.folderIndex) 1 else -1
                    actions.onFolderSelect(index)
                },
            )
            libraryModeGridItems(content, layout, actions, historyCategory, rows, queueRows, switchItem)
        }
    }
}

/** 列表项：首屏骨架、视频行、空态和分页尾巴。 */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.libraryModeGridItems(
    content: LibraryModeContent, layout: LibraryModeLayout, actions: LibraryModeActions,
    historyCategory: HistoryCategory, rows: List<com.aritxonly.bdfun.data.VideoItem>,
    queueRows: List<com.aritxonly.bdfun.data.VideoItem>, itemModifier: Modifier,
) {
    val emptyFirstLoad = content.loading && rows.isEmpty()
    if (emptyFirstLoad) {
        videoResultSkeleton(keyPrefix = "library-skeleton-${layout.mode}")
    }
    videoResultRows(
        videos = rows,
        keyPrefix = "library-${layout.mode}-",
        // 队列必须在点击时、跳转前就准备好：一镜到底会由系统动画自己起 VideoActivity，
        // onOpenVideo 只是动画失败时的兜底，正常路径根本不执行，写在那里等于没准备。
        onBeforeOpen = { video ->
            if (!content.batchMode) {
                // 稍后再看把整页当成一个本地合集；收藏/历史只保留普通连播队列。
                if (layout.mode == 2) {
                    actions.onPrepareCollection(queueRows, video.bvid, "稍后再看")
                } else {
                    actions.onPrepareQueue(queueRows, video.bvid, "")
                }
            }
        },
        onOpenVideo = { video ->
            if (content.batchMode) {
                actions.onToggleSelected(video.bvid)
            } else {
                actions.onOpenVideo(video)
            }
        },
        onVideoMenu = if (content.batchMode) null else actions.onVideoMenu,
        showSelection = content.batchMode,
        selectedIds = content.selected,
        // 批量勾选时不要一镜到底放大动画。
        enableOneTake = !content.batchMode,
        itemModifier = itemModifier,
    )
    if (!emptyFirstLoad && rows.isEmpty() && !content.loading && content.error == null) {
        libraryEmptyItem(layout.mode, historyCategory)
    }
    libraryGridFooter(content, layout, actions)
}

/** 空列表提示：放在内容区中间，而不是缩在左上角。 */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.libraryEmptyItem(
    mode: Int, historyCategory: HistoryCategory,
) {
    item(key = "library-empty", span = { GridItemSpan(maxLineSpan) }) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth().height(320.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    mode == 0 -> "这个收藏夹还没有视频"
                    mode == 1 && historyCategory != HistoryCategory.All -> "该分类下暂无观看记录"
                    else -> "暂时没有内容"
                },
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp,
            )
        }
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
    historyCategory: HistoryCategory = HistoryCategory.All,
    onHistoryCategory: (HistoryCategory) -> Unit = {},
    onFolderSelect: ((Int) -> Unit)? = null,
) {
    val title = layout.title; val mode = layout.mode; val showModeTabs = layout.showModeTabs
    val modeProgress = layout.modeProgress; val folders = content.folders; val foldersReady = content.foldersReady
    val folderIndex = content.folderIndex; val onSelectMode = actions.onSelectMode
    val selectFolder = onFolderSelect ?: actions.onFolderSelect
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

            if (mode == 1) {
                item(key = "history-categories", span = { GridItemSpan(maxLineSpan) }) {
                    LibraryHistoryCategoryBar(historyCategory, onHistoryCategory)
                }
            }

            if (mode == 0 && folders.isNotEmpty() && foldersReady) {
                item(key = "folders", span = { GridItemSpan(maxLineSpan) }) {
                    // 首次加载完成后再出现；之后切换收藏夹、重载列表时保持原位，不重播动画。
                    LibraryFolderBar(
                        folders = folders,
                        selectedIndex = folderIndex,
                        onSelect = selectFolder,
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
