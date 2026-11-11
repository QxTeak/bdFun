package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TeakTopGlassButton
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.navigation.TeakBatchAction
import com.aritxonly.teak.common.navigation.TeakBatchActionBar
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import com.aritxonly.teak.common.navigation.TeakMorphMenuAnchor
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import com.kyant.shapes.Capsule
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.runtime.CompositionLocalProvider

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.zIndex
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.FavoriteFolder
import com.aritxonly.bdfun.data.HistoryItem
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.data.VideoPlayQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val MODES = listOf("收藏", "历史", "稍后再看")

/** 收藏 / 历史页顶栏按钮和页面内部动作之间的轻量事件总线。 */
object LibraryPageBus {
    var scrollTopRequest by mutableIntStateOf(0)
        private set
    var batchRequest by mutableIntStateOf(0)
        private set
    var searchRequest by mutableIntStateOf(0)
        private set

    /** 批量操作条挂在 Activity 的 overlay 层，状态通过这里和页面同步。 */
    var batchVisible by mutableStateOf(false)
    var batchCount by mutableIntStateOf(0)
    var batchCanTransfer by mutableStateOf(false)
    var batchActionRequest by mutableIntStateOf(0)
        private set
    var batchActionKey by mutableStateOf("")

    fun requestScrollTop() { scrollTopRequest++ }
    fun requestBatch() { batchRequest++ }
    fun requestSearch() { searchRequest++ }
    fun sendBatchAction(key: String) {
        batchActionKey = key
        batchActionRequest++
    }
    fun clearBatch() {
        batchVisible = false
        batchCount = 0
        batchCanTransfer = false
        batchActionKey = ""
    }
}

@Composable
fun LibraryTopActions(menuState: TeakMorphMenuState) {
    TeakTopGlassButton(onClick = { LibraryPageBus.requestSearch() }) {
        LibrarySearchGlyph()
    }
    // 省略号现在走 BiliPai 同款形变菜单：按钮被吸进玻璃，菜单从按钮位置长出来。
    TeakMorphMenuAnchor(menuState) { onClick ->
        TeakTopGlassButton(onClick = onClick) {
            LibraryThreeDotsGlyph()
        }
    }
}

/** 顶栏省略号菜单的内容；动作在收回动画结束后才执行。 */
@Composable
fun LibraryMorphMenuContent(dismiss: (after: () -> Unit) -> Unit) {
    LibraryMenuRow("回到顶部") {
        dismiss { LibraryPageBus.requestScrollTop() }
    }
    LibraryMenuRow("批量管理") {
        dismiss { LibraryPageBus.requestBatch() }
    }
}

/**
 * 批量操作条放在 Activity overlay 层：这里在页面录制层外面，
 * 可以直接用页面已有的 backdrop 做真玻璃，不需要再录一层。
 */
@Composable
fun LibraryBatchOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        TeakBatchActionBar(
            visible = LibraryPageBus.batchVisible,
            selectedCount = LibraryPageBus.batchCount,
            actions = buildList {
                add(TeakBatchAction("all", "全选"))
                add(
                    TeakBatchAction(
                        "delete",
                        "删除",
                        enabled = LibraryPageBus.batchCount > 0,
                        danger = true,
                    ),
                )
                if (LibraryPageBus.batchCanTransfer) {
                    add(
                        TeakBatchAction(
                            "move",
                            "移动到",
                            enabled = LibraryPageBus.batchCount > 0,
                        ),
                    )
                    add(
                        TeakBatchAction(
                            "copy",
                            "复制到",
                            enabled = LibraryPageBus.batchCount > 0,
                        ),
                    )
                }
            },
            onAction = { LibraryPageBus.sendBatchAction(it.key) },
            modifier = Modifier
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 14.dp),
        )
    }
}

/**
 * 放大镜：圆环 + 短手柄，线条比原来的实心圆更细、更接近系统搜索图标（参考图二）。
 * 圆环用描边而不是填充，手柄与圆环在 45° 处相接。
 */
@Composable
private fun LibrarySearchGlyph() {
    top.yukonga.miuix.kmp.basic.Icon(
        imageVector = top.yukonga.miuix.kmp.icon.MiuixIcons.Search,
        contentDescription = "搜索",
        tint = teakGlassGlyphColor(),
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun LibraryThreeDotsGlyph() {
    top.yukonga.miuix.kmp.basic.Icon(
        imageVector = top.yukonga.miuix.kmp.icon.MiuixIcons.More,
        contentDescription = "更多",
        tint = teakGlassGlyphColor(),
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun LibraryMenuRow(label: String, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = label, color = colors.onBackground, fontSize = 14.sp)
    }
}

@Composable
fun LibraryScreen(
    api: BiliApi,
    loggedIn: Boolean,
    onNeedLogin: () -> Unit,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    /** 非空表示只显示这一个分类（收藏=0 / 历史=1 / 稍后再看=2），并隐藏切换标签。 */
    fixedMode: Int? = null,
    title: String = "收藏",
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pageCount = if (fixedMode == null) MODES.size else 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val currentMode = fixedMode ?: pagerState.currentPage

    val settings = com.aritxonly.bdfun.AppGraph.settings
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var batchMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    /** 1 = 移动到，2 = 复制到；null 表示面板关闭。 */
    var transferMode by remember { mutableStateOf<Int?>(null) }

    // 收藏
    var folders by remember { mutableStateOf<List<FavoriteFolder>>(emptyList()) }
    var folderIndex by remember { mutableIntStateOf(0) }
    var folderVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var folderPage by remember { mutableIntStateOf(1) }
    var folderHasMore by remember { mutableStateOf(false) }
    var loadingMoreFolders by remember { mutableStateOf(false) }
    var historyMax by remember { mutableLongStateOf(0L) }
    var historyViewAt by remember { mutableLongStateOf(0L) }
    var historyBusiness by remember { mutableStateOf("") }
    var historyHasMore by remember { mutableStateOf(false) }
    var loadingMoreHistory by remember { mutableStateOf(false) }
    // 首次收藏夹内容加载完成之前不显示收藏夹栏；之后切换收藏夹时保持不动。
    var foldersReady by remember { mutableStateOf(false) }
    // 历史 / 稍后再看
    var historyEntries by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var watchLaterEntries by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var watchLaterVisibleCount by remember { mutableIntStateOf(20) }

    val loadingTabs = remember { mutableStateMapOf<Int, Boolean>() }
    val errorTabs = remember { mutableStateMapOf<Int, String?>() }

    var consumedScrollTop by remember { mutableIntStateOf(LibraryPageBus.scrollTopRequest) }
    var consumedBatch by remember { mutableIntStateOf(LibraryPageBus.batchRequest) }
    var consumedSearch by remember { mutableIntStateOf(LibraryPageBus.searchRequest) }
    var scrollTopTick by remember { mutableIntStateOf(0) }

    // 手指滑到另一页时，清掉上一页留下的勾选/批量状态。
    LaunchedEffect(pagerState.currentPage) {
        selected = emptySet()
        batchMode = false
    }

    BackHandler {
        if (transferMode != null) {
            transferMode = null
        } else if (searching || batchMode) {
            searching = false
            batchMode = false
            selected = emptySet()
            query = ""
        } else {
            onBack()
        }
    }

    LaunchedEffect(LibraryPageBus.scrollTopRequest) {
        val tick = LibraryPageBus.scrollTopRequest
        if (tick == consumedScrollTop) return@LaunchedEffect
        consumedScrollTop = tick
        if (tick > 0) scrollTopTick = tick
    }
    LaunchedEffect(LibraryPageBus.batchRequest) {
        val tick = LibraryPageBus.batchRequest
        if (tick == consumedBatch) return@LaunchedEffect
        consumedBatch = tick
        if (tick > 0) {
            batchMode = !batchMode
            selected = emptySet()
        }
    }
    LaunchedEffect(LibraryPageBus.searchRequest) {
        val tick = LibraryPageBus.searchRequest
        if (tick == consumedSearch) return@LaunchedEffect
        consumedSearch = tick
        if (tick > 0) {
            searching = !searching
            if (!searching) query = ""
        }
    }

    // 把批量状态同步到 Activity overlay 层的操作条。
    LaunchedEffect(batchMode, selected.size, currentMode) {
        LibraryPageBus.batchVisible = batchMode
        LibraryPageBus.batchCount = selected.size
        LibraryPageBus.batchCanTransfer = currentMode == 0
    }
    DisposableEffect(Unit) {
        onDispose { LibraryPageBus.clearBatch() }
    }

    suspend fun loadFolder(index: Int) {
        val folder = folders.getOrNull(index) ?: return
        loadingTabs[0] = true
        errorTabs[0] = null
        val page = runCatching { api.favoriteVideosPage(folder.id, 1) }
            .onFailure { errorTabs[0] = it.message ?: "收藏夹加载失败" }
            .getOrNull()
        folderVideos = page?.first ?: emptyList()
        folderPage = 1
        folderHasMore = page?.second == true
        loadingTabs[0] = false
    }

    suspend fun loadMoreFolders() {
        if (!folderHasMore || loadingMoreFolders) return
        val folder = folders.getOrNull(folderIndex) ?: return
        loadingMoreFolders = true
        val next = folderPage + 1
        val page = runCatching { api.favoriteVideosPage(folder.id, next) }.getOrNull()
        if (page != null) {
            val existing = folderVideos.mapTo(HashSet()) { it.bvid }
            folderVideos = folderVideos + page.first.filter { existing.add(it.bvid) }
            folderPage = next
            folderHasMore = page.second
        }
        loadingMoreFolders = false
    }

    suspend fun loadFavorites() {
        loadingTabs[0] = true
        errorTabs[0] = null
        folders = runCatching { api.favoriteFolders() }
            .onFailure { errorTabs[0] = it.message ?: "收藏夹加载失败" }
            .getOrDefault(emptyList())
        if (folders.isEmpty()) {
            errorTabs[0] = errorTabs[0] ?: "还没有收藏夹"
            loadingTabs[0] = false
        } else {
            folderIndex = 0
            loadFolder(0)
            foldersReady = true
        }
    }

    suspend fun loadHistory() {
        loadingTabs[1] = true
        errorTabs[1] = null
        val page = runCatching { api.historyPage(0L, 0L) }
            .onFailure { errorTabs[1] = it.message ?: "历史记录加载失败" }
            .getOrNull()
        val loaded = page?.items.orEmpty()
        historyMax = page?.max ?: 0L
        historyViewAt = page?.viewAt ?: 0L
        historyBusiness = page?.business.orEmpty()
        // 接口的 has_more 字段偶尔缺失或类型不一致；先允许再翻一页，
        // 下一页若无新内容会在 loadMore 里自动停止。
        historyHasMore = loaded.isNotEmpty()
        // 历史接口本身不带播放量/弹幕数：先补齐再显示，避免列表出现后整行统计突然跳出来。
        val enriched = if (loaded.isEmpty()) {
            loaded
        } else {
            runCatching { api.enrichHistoryStats(loaded) }.getOrDefault(loaded)
        }
        historyEntries = enriched
        loadingTabs[1] = false
    }

    suspend fun loadMoreHistory() {
        if (!historyHasMore || loadingMoreHistory) return
        loadingMoreHistory = true
        val page = runCatching {
            api.historyPage(historyMax, historyViewAt, historyBusiness)
        }.getOrNull()
        if (page != null) {
            val enriched = runCatching { api.enrichHistoryStats(page.items) }
                .getOrDefault(page.items)
            val existing = historyEntries.mapTo(HashSet()) { it.bvid }
            val added = enriched.filter { existing.add(it.bvid) }
            historyEntries = historyEntries + added
            historyMax = page.max
            historyViewAt = page.viewAt
            historyHasMore = added.isNotEmpty()
            com.aritxonly.bdfun.data.LogCollector.record(
                "HISTORY",
                "loadMore max=${page.max} viewAt=${page.viewAt} items=${page.items.size} " +
                    "added=${added.size} hasMore=$historyHasMore",
            )
        }
        loadingMoreHistory = false
    }

    suspend fun loadWatchLater() {
        loadingTabs[2] = true
        errorTabs[2] = null
        watchLaterEntries = runCatching { api.watchLater() }
            .onFailure { errorTabs[2] = it.message ?: "稍后再看加载失败" }
            .getOrDefault(emptyList())
        watchLaterVisibleCount = 20
        loadingTabs[2] = false
    }

    LaunchedEffect(loggedIn, fixedMode) {
        if (!loggedIn) return@LaunchedEffect
        // 三页并行预取，左右滑动时卡片已经准备好。
        when (fixedMode) {
            0 -> launch { loadFavorites() }
            1 -> launch { loadHistory() }
            2 -> launch { loadWatchLater() }
            else -> {
                launch { loadFavorites() }
                launch { loadHistory() }
                launch { loadWatchLater() }
            }
        }
    }

    val visibleVideos = if (query.isBlank()) {
        folderVideos
    } else {
        folderVideos.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.ownerName.contains(query, ignoreCase = true)
        }
    }
    fun visibleEntries(entries: List<HistoryItem>): List<HistoryItem> = if (query.isBlank()) {
        entries
    } else {
        entries.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
        }
    }

    fun idsOf(mode: Int): List<String> = when (mode) {
        0 -> visibleVideos.map { it.bvid }
        1 -> visibleEntries(historyEntries).map { it.bvid }
        else -> visibleEntries(watchLaterEntries).map { it.bvid }
    }

    fun deleteSelected(mode: Int) {
        val ids = selected
        if (ids.isEmpty()) return
        batchMode = false
        selected = emptySet()
        scope.launch {
            var allOk = true
            when (mode) {
                0 -> {
                    val folderId = folders.getOrNull(folderIndex)?.id
                    if (folderId != null) {
                        folderVideos.filter { it.bvid in ids }.forEach { video ->
                            val ok = api.removeFavoriteAll(video.aid, listOf(folderId))
                            allOk = allOk && ok
                        }
                        folderVideos = folderVideos.filterNot { it.bvid in ids }
                    }
                }

                1 -> {
                    historyEntries.filter { it.bvid in ids }.forEach { entry ->
                        val ok = entry.aid > 0 && api.deleteHistory(entry.aid)
                        allOk = allOk && ok
                    }
                    historyEntries = historyEntries.filterNot { it.bvid in ids }
                }

                else -> {
                    watchLaterEntries.filter { it.bvid in ids }.forEach { entry ->
                        val ok = if (entry.aid > 0) {
                            api.deleteWatchLater(entry.aid)
                        } else {
                            false
                        }
                        allOk = allOk && ok
                    }
                    watchLaterEntries = watchLaterEntries.filterNot { it.bvid in ids }
                }
            }
            context.showToast(
                if (allOk) "已删除 ${ids.size} 项" else "部分删除失败，请重试",
            )
        }
    }

    /** 把勾选的视频复制 / 移动到另一个收藏夹。 */
    fun transferSelected(kind: Int, target: FavoriteFolder) {
        val ids = selected
        if (ids.isEmpty()) return
        val sourceFolderId = folders.getOrNull(folderIndex)?.id
        val moving = kind == 1
        transferMode = null
        batchMode = false
        selected = emptySet()
        scope.launch {
            val chosen = folderVideos.filter { it.bvid in ids }
            var allOk = true
            chosen.forEach { video ->
                val added = runCatching { api.setFavorite(video.aid, target.id, true) }
                    .getOrDefault(false)
                allOk = allOk && added
                if (added && moving && sourceFolderId != null) {
                    val removed = runCatching {
                        api.removeFavoriteAll(video.aid, listOf(sourceFolderId))
                    }.getOrDefault(false)
                    allOk = allOk && removed
                }
            }
            if (moving) folderVideos = folderVideos.filterNot { it.bvid in ids }
            context.showToast(
                if (allOk) {
                    if (moving) "已移动到「${target.title}」" else "已复制到「${target.title}」"
                } else {
                    "部分操作失败，请重试"
                },
            )
        }
    }

    // 操作条在 overlay 层，点击后通过总线回到页面里执行。
    var consumedBatchAction by remember { mutableIntStateOf(LibraryPageBus.batchActionRequest) }
    LaunchedEffect(LibraryPageBus.batchActionRequest) {
        val tick = LibraryPageBus.batchActionRequest
        if (tick == consumedBatchAction) return@LaunchedEffect
        consumedBatchAction = tick
        when (LibraryPageBus.batchActionKey) {
            "all" -> {
                val all = idsOf(currentMode)
                selected = if (all.isNotEmpty() && selected.size == all.size) {
                    emptySet()
                } else {
                    all.toSet()
                }
            }
            "delete" -> deleteSelected(currentMode)
            "move" -> if (currentMode == 0) transferMode = 1
            "copy" -> if (currentMode == 0) transferMode = 2
        }
    }

    if (!loggedIn) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "登录后可查看收藏与历史",
                color = colors.onBackgroundVariant,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable { onNeedLogin() }
                    .padding(horizontal = 26.dp, vertical = 11.dp),
            ) {
                Text(
                    text = "扫码登录",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
        return
    }

    // 三个分类横向相连：手指拖到哪，相邻分类的卡片就跟到哪；Tab 点击也是同一套弹簧动画。
    val modeProgress = remember {
        derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction }
    }
    Box(modifier = Modifier.fillMaxSize()) {
    // 和首页一样把翻页触摸阈值放大一点，避免手指轻抖就横划换分类。
    CompositionLocalProvider(
        LocalViewConfiguration provides rememberStricterSwipeConfiguration(),
    ) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        beyondViewportPageCount = 0,
        overscrollEffect = null,
        pageSpacing = 12.dp,
    ) { page ->
        val mode = fixedMode ?: page
        LibraryModePage(
            mode = mode,
            title = title,
            showModeTabs = fixedMode == null,
            isActive = mode == currentMode,
            searching = searching,
            query = query,
            onQueryChange = { query = it },
            onCloseSearch = {
                searching = false
                query = ""
            },
            onSelectMode = { target ->
                selected = emptySet()
                batchMode = false
                scope.launch { pagerState.animateScrollToPage(target) }
            },
            modeProgress = modeProgress,
            scrollTopTick = scrollTopTick,
            folders = folders,
            foldersReady = foldersReady,
            folderIndex = folderIndex,
            folderVideos = visibleVideos,
            historyEntries = visibleEntries(historyEntries),
            watchLaterEntries = visibleEntries(watchLaterEntries).take(watchLaterVisibleCount),
            loading = loadingTabs[mode] == true,
            error = errorTabs[mode],
            batchMode = batchMode,
            selected = selected,
            onToggleSelected = { id -> selected = toggleSelection(selected, id) },
            onSelectAll = {
                val all = idsOf(mode)
                selected = if (all.isNotEmpty() && selected.size == all.size) {
                    emptySet()
                } else {
                    all.toSet()
                }
            },
            onDelete = { deleteSelected(mode) },
            onCancelBatch = {
                batchMode = false
                selected = emptySet()
            },
            onFolderSelect = { index ->
                folderIndex = index
                selected = emptySet()
                scope.launch { loadFolder(index) }
            },
            onOpenVideo = onOpenVideo,
            onLoadMore = {
                when (mode) {
                    0 -> scope.launch { loadMoreFolders() }
                    1 -> scope.launch { loadMoreHistory() }
                    2 -> watchLaterVisibleCount = (watchLaterVisibleCount + 20)
                        .coerceAtMost(watchLaterEntries.size)
                }
            },
            canLoadMore = when (mode) {
                0 -> folderHasMore
                1 -> historyHasMore
                else -> watchLaterVisibleCount < watchLaterEntries.size
            },
        )
    }
    }

    if (searching) {
        LibrarySearchPage(
            mode = currentMode,
            title = title,
            history = settings.searchHistory,
            folders = folders,
            folderIndex = folderIndex,
            folderVideos = visibleVideos,
            historyEntries = visibleEntries(historyEntries),
            watchLaterEntries = visibleEntries(watchLaterEntries).take(watchLaterVisibleCount),
            loading = loadingTabs[currentMode] == true,
            error = errorTabs[currentMode],
            onClearHistory = {
                settings.clearSearchHistory()
                query = ""
            },
            onSubmit = { word ->
                val q = word.trim()
                if (q.isNotEmpty()) {
                    settings.addSearchHistory(q)
                    query = q
                }
            },
            onBack = {
                searching = false
                query = ""
            },
            onOpenVideo = onOpenVideo,
        )
    }

    // 移动 / 复制：与视频页点「收藏」后弹出的收藏夹菜单同一套底部面板。
    val activeTransfer = transferMode
    val sourceId = folders.getOrNull(folderIndex)?.id
    val targets = folders.filter { it.id != sourceId }
    com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheet(
        show = activeTransfer != null,
        state = com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheetUiState(
            title = if (activeTransfer == 1) "移动到收藏夹" else "复制到收藏夹",
            description = "选择目标收藏夹 · 已选择 ${selected.size} 个视频",
            folders = targets.map { folder ->
                com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderOption(
                    folder.id, folder.title, folder.count,
                )
            },
            emptyMessage = "没有其他可用的收藏夹",
        ),
        onDismiss = { transferMode = null },
        onSelect = { id -> targets.firstOrNull { it.id == id }?.let { transferSelected(activeTransfer ?: 2, it) } },
    )
    }
}

@Composable
private fun LibraryCardEntrance(
    index: Int,
    itemKey: String,
    content: @Composable () -> Unit,
) {
    val isNew = remember(itemKey) { LibraryEntranceMemory.seen.add(itemKey) }
    var started by remember(itemKey) { mutableStateOf(!isNew) }
    LaunchedEffect(itemKey, isNew) {
        if (isNew) {
            kotlinx.coroutines.delay(kotlin.math.min(index / 2, 6) * 70L)
            started = true
        }
    }
    val progress = rememberManualProgress(if (started) 1f else 0f, 140f)
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 12.dp.toPx()
        },
    ) {
        content()
    }
}

private object LibraryEntranceMemory {
    val seen = HashSet<String>()
}

@Composable
private fun LibraryModePage(
    mode: Int,
    title: String,
    showModeTabs: Boolean,
    isActive: Boolean,
    searching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onSelectMode: (Int) -> Unit,
    modeProgress: State<Float>,
    scrollTopTick: Int,
    folders: List<FavoriteFolder>,
    foldersReady: Boolean,
    folderIndex: Int,
    folderVideos: List<VideoItem>,
    historyEntries: List<HistoryItem>,
    watchLaterEntries: List<HistoryItem>,
    loading: Boolean,
    error: String?,
    batchMode: Boolean,
    selected: Set<String>,
    onToggleSelected: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onCancelBatch: () -> Unit,
    onFolderSelect: (Int) -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onLoadMore: () -> Unit,
    canLoadMore: Boolean,
) {
    val colors = MiuixTheme.colorScheme
    val gridState = rememberLazyGridState()
    RegisterScrollTop(gridState, "library")
    if (isActive) TrackTeakPageScroll(gridState)
    if (isActive) {
        LaunchedEffect(gridState) {
            snapshotFlow {
                gridState.layoutInfo.totalItemsCount to gridState.canScrollForward
            }.collect { (total, canScroll) ->
                // 数据刚加载完时 total 会变化；即使列表短到不能滚动，也要再试一次下一页。
                if (total > 0 && !canScroll) onLoadMore()
            }
        }
    }

    // 只在当前页响应「回到顶部」；切换页时把旧请求标记为已看过，避免之后突然跳顶。
    var seenScrollTop by remember { mutableIntStateOf(scrollTopTick) }
    LaunchedEffect(scrollTopTick, isActive) {
        if (!isActive) {
            seenScrollTop = scrollTopTick
            return@LaunchedEffect
        }
        if (scrollTopTick == seenScrollTop) return@LaunchedEffect
        seenScrollTop = scrollTopTick
        if (scrollTopTick > 0) gridState.animateScrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = TeakChromeDefaults.ContentHorizontalPadding,
                end = TeakChromeDefaults.ContentHorizontalPadding,
                top = TeakChromeDefaults.ContentTopPadding,
                bottom = if (batchMode) 92.dp else TeakChromeDefaults.ContentBottomPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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

            if (mode == 0) {
                itemsIndexed(items = folderVideos, key = { _, video -> video.bvid }) { index, video ->
                    val oneTake = rememberOneTakeAnchor()
                    val itemContext = LocalContext.current
                    val cardRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
                    LibraryCardEntrance(index, "0-${video.bvid}") {
                    VideoGridCard(
                        video = video,
                        modifier = oneTake.modifier,
                        onClick = {
                            if (batchMode) {
                                onToggleSelected(video.bvid)
                            } else {
                                VideoPlayQueue.set(folderVideos, video.bvid)
                                oneTake.open(itemContext, video, cardRadiusPx) { onOpenVideo(video) }
                            }
                        },
                        selectionEnabled = batchMode,
                        selected = video.bvid in selected,
                    )
                    }
                }
            } else {
                val entries = if (mode == 1) historyEntries else watchLaterEntries
                itemsIndexed(items = entries, key = { _, entry -> entry.bvid }) { index, entry ->
                    val video = entry.toVideoItem()
                    val oneTake = rememberOneTakeAnchor()
                    val itemContext = LocalContext.current
                    val cardRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
                    LibraryCardEntrance(index, "${mode}-${entry.bvid}") {
                    VideoGridCard(
                        video = video,
                        modifier = oneTake.modifier,
                        onClick = {
                            if (batchMode) {
                                onToggleSelected(entry.bvid)
                            } else {
                                val queue = if (mode == 1) {
                                    historyEntries.map { it.toVideoItem() }
                                } else {
                                    watchLaterEntries.map { it.toVideoItem() }
                                }
                                VideoPlayQueue.set(queue, entry.bvid)
                                oneTake.open(itemContext, video, cardRadiusPx) { onOpenVideo(video) }
                            }
                        },
                        progress = if (entry.duration > 0) {
                            entry.progress.toFloat() / entry.duration
                        } else {
                            0f
                        },
                        selectionEnabled = batchMode,
                        selected = entry.bvid in selected,
                    )
                    }
                }
            }

            if (canLoadMore) {
                item(key = "load-more", span = { GridItemSpan(maxLineSpan) }) {
                    // 滑到底时这个哨兵会被组合，随即请求下一页；用列表长度做 key，
                    // 新数据到达后如果还没到底，会再次触发。
                    val dataSize = if (mode == 0) folderVideos.size else if (mode == 1) {
                        historyEntries.size
                    } else {
                        watchLaterEntries.size
                    }
                    LaunchedEffect(dataSize) {
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
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (loading) {
            // 收藏 / 历史 / 稍后再看：小电视放在内容区域正中，并缩小一号
            TeakLoading(
                modifier = Modifier.align(Alignment.Center),
                color = colors.primary,
                size = 48.dp,
            )
        }

    }
}

private fun toggleSelection(current: Set<String>, id: String): Set<String> =
    if (id in current) current - id else current + id

internal fun HistoryItem.toVideoItem(): VideoItem = VideoItem(
    bvid = bvid,
    aid = aid,
    title = title,
    cover = cover,
    ownerName = author,
    ownerFace = "",
    ownerMid = 0,
    play = play.toLong(),
    danmaku = danmaku.toLong(),
    duration = duration,
    pubdate = viewedAt,
)

/** 收藏夹分类：与个人页投稿页「合集与系列」同款胶囊。 */
@Composable
private fun LibraryFolderBar(
    folders: List<FavoriteFolder>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex, folders.size) {
        if (selectedIndex in folders.indices) {
            runCatching { listState.animateScrollToItem(selectedIndex) }
        }
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(folders) { index, folder ->
            LibraryFolderChip(
                text = "${folder.title} ${folder.count}",
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

/** 与个人页 SubmissionChip 完全一致：选中粉底白字，未选中白底黑字。 */
@Composable
private fun LibraryFolderChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.primary else colors.surface)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(50),
            )
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else colors.onBackground,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun LibraryBatchAction(
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (enabled) color else color.copy(alpha = 0.4f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
