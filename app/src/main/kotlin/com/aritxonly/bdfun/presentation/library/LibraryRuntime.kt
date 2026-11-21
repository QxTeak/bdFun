package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.model.library.FavoriteFolder
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.library.application.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class LibraryRuntime(
    val data: LibraryStateHolder, val interaction: LibraryInteractionStateHolder,
    val pager: androidx.compose.foundation.pager.PagerState,
    val chrome: PageChromePresentationState, val presentation: LibraryPresentationStateHolder,
    val scope: kotlinx.coroutines.CoroutineScope, val context: android.content.Context,
    val dependencies: LibraryDependencies, val navigation: LibraryNavigation, val request: LibraryScreenRequest,
) {
    val currentMode get() = request.fixedMode ?: pager.currentPage
    suspend fun loadWatchLater() { data.loadWatchLater(); interaction.resetWatchLaterCount() }
    fun loadMore(mode: Int) {
        when(mode) {
            0 -> scope.launch { data.loadMoreFolders() }
            1 -> scope.launch { data.loadMoreHistory() }
            2 -> interaction.showMoreWatchLater(data.state.value.watchLater.size)
        }
    }
    fun selectMode(target: Int) { interaction.pageChanged(); scope.launch { pager.animateScrollToPage(target) } }
    fun selectFolder(index: Int) { interaction.selectFolder(); scope.launch { data.loadFolder(index) } }
    fun batchAction(key: String) {
        val selected = interaction.batchAction(key, currentMode, data.state.value) ?: return
        scope.launch {
            data.deleteSelected(selected.mode, selected.ids)?.let { context.showToast(it) }
            // 删除会改变当前收藏夹的数量，顶部的收藏夹栏要跟着更新。
            data.refreshFolderCounts()
        }
    }
    /** 长按/省略号：打开和首页同款的锚定下拉菜单。 */
    fun openSheet(video: VideoItem, anchor: androidx.compose.ui.geometry.Rect) {
        presentation.sheetAnchor = anchor
        presentation.sheetVideo = video
    }
    fun dismissSheet() { presentation.sheetVideo = null }
    /** 面板里的选项直接复用批量管理：只选中这一条再走原来的删除/移动/复制流程。 */
    fun sheetAction(kind: String, video: VideoItem) {
        presentation.sheetVideo = null
        interaction.selectOnly(video.bvid)
        batchAction(kind)
    }
    fun transfer(kind: Int, target: FavoriteFolder) {
        val selected = interaction.beginTransfer(kind) ?: return
        scope.launch {
            data.transferSelected(selected.moving, target, selected.ids)?.let { context.showToast(it) }
            // 移动/复制后来源和目标收藏夹的数量都会变，刷新顶部收藏夹栏。
            data.refreshFolderCounts()
        }
    }
}

@Composable
internal fun rememberLibraryRuntime(
    request: LibraryScreenRequest, navigation: LibraryNavigation, dependencies: LibraryDependencies,
): LibraryRuntime {
    val chrome = LocalPageChromeState.current
    val parent = rememberCoroutineScope()
    val data = remember(dependencies, request.owner, request.loggedIn, request.sessionRevision) { dependencies.createData() }
    val scope = remember(data) { kotlinx.coroutines.CoroutineScope(parent.coroutineContext +
        kotlinx.coroutines.SupervisorJob(parent.coroutineContext[kotlinx.coroutines.Job])) }
    val interaction = remember(data) { dependencies.createInteraction(scope) }
    DisposableEffect(data, interaction) { onDispose { data.close(); interaction.close(); scope.cancel() } }
    val presentation = remember(chrome) { LibraryPresentationStateHolder(chrome) }
    val count = if (request.fixedMode == null) MODES.size else 1
    val pager = rememberPagerState(pageCount = { count })
    return LibraryRuntime(data, interaction, pager, chrome, presentation, scope, LocalContext.current, dependencies, navigation, request)
}
