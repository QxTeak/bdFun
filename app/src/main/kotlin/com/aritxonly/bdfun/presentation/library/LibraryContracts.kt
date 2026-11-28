package com.aritxonly.bdfun.ui

import androidx.compose.runtime.State
import com.aritxonly.bdfun.core.model.library.FavoriteFolder
import com.aritxonly.bdfun.core.model.library.HistoryItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.library.application.*

internal val MODES = listOf("收藏", "历史", "稍后再看")
internal data class LibraryScreenRequest(val loggedIn: Boolean, val owner: Long?, val fixedMode: Int? = null, val title: String = "收藏", val sessionRevision: Long = 0)
internal class LibraryNavigation(val onNeedLogin: () -> Unit, val onBack: () -> Unit, val onOpenVideo: (VideoItem) -> Unit)
internal class LibraryDependencies(
    val createData: () -> LibraryStateHolder,
    val createInteraction: (kotlinx.coroutines.CoroutineScope, String) -> LibraryInteractionStateHolder,
    val playbackQueue: com.aritxonly.bdfun.core.application.PlaybackQueueStateHolder,
    val localCollection: com.aritxonly.bdfun.core.application.LocalVideoCollectionStateHolder,
)
internal data class LibraryModeContent(
    val folders: List<FavoriteFolder>, val foldersReady: Boolean, val folderIndex: Int,
    val folderVideos: List<VideoItem>, val historyEntries: List<HistoryItem>, val watchLaterEntries: List<HistoryItem>,
    val loading: Boolean, val error: String?, val batchMode: Boolean, val selected: Set<String>, val canLoadMore: Boolean,
    /** 稍后再看的完整列表：进视频时整页加入播放队列，连播不会只覆盖当前已加载的前 20 条。 */
    val allWatchLater: List<VideoItem> = emptyList(),
)
/** 用户手动删除的 key 与序号：只用来触发删除粒子，换页/刷新时不带。 */
internal data class LibraryRemoval(val seq: Int, val keys: Set<String>)
internal data class LibraryModeLayout(
    val mode: Int, val title: String, val showModeTabs: Boolean, val isActive: Boolean,
    val modeProgress: State<Float>, val scrollTopTick: Int,
    val removal: LibraryRemoval? = null,
)
internal class LibraryModeActions(
    val owner: Any, val onSelectMode: (Int) -> Unit, val onToggleSelected: (String) -> Unit,
    val onFolderSelect: (Int) -> Unit, val onOpenVideo: (VideoItem) -> Unit, val onLoadMore: () -> Unit,
    val onPrepareQueue: (List<VideoItem>, String, String) -> Unit,
    /** 稍后再看等入口：整页作为本地合集。 */
    val onPrepareCollection: (List<VideoItem>, String, String) -> Unit,
    /** 长按视频：弹出和首页同款的锚定下拉菜单（选项取批量管理的功能）。 */
    val onLongPressVideo: (VideoItem) -> Unit = {},
    /** 省略号与长按共用同一菜单；坐标用于锚定。 */
    val onVideoMenu: (VideoItem, androidx.compose.ui.geometry.Rect) -> Unit = { _, _ -> },
)
