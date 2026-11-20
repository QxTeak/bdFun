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
    val createInteraction: (kotlinx.coroutines.CoroutineScope) -> LibraryInteractionStateHolder,
    val playbackQueue: com.aritxonly.bdfun.core.application.PlaybackQueueStateHolder,
)
internal data class LibraryModeContent(
    val folders: List<FavoriteFolder>, val foldersReady: Boolean, val folderIndex: Int,
    val folderVideos: List<VideoItem>, val historyEntries: List<HistoryItem>, val watchLaterEntries: List<HistoryItem>,
    val loading: Boolean, val error: String?, val batchMode: Boolean, val selected: Set<String>, val canLoadMore: Boolean,
)
internal data class LibraryModeLayout(
    val mode: Int, val title: String, val showModeTabs: Boolean, val isActive: Boolean,
    val modeProgress: State<Float>, val scrollTopTick: Int,
)
internal class LibraryModeActions(
    val owner: Any, val onSelectMode: (Int) -> Unit, val onToggleSelected: (String) -> Unit,
    val onFolderSelect: (Int) -> Unit, val onOpenVideo: (VideoItem) -> Unit, val onLoadMore: () -> Unit,
    val onPrepareQueue: (List<VideoItem>, String) -> Unit,
    /** 长按视频：弹出和首页同款的底部操作面板（选项取批量管理的功能）。 */
    val onLongPressVideo: (VideoItem) -> Unit = {},
)
