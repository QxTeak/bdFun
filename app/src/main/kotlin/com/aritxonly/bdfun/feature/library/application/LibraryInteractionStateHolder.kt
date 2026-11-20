package com.aritxonly.bdfun.feature.library.application

import com.aritxonly.bdfun.core.domain.GeneralSettings
import com.aritxonly.bdfun.core.model.library.HistoryItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal data class LibraryInteractionState(
    val searchHistory: List<String>, val searching: Boolean = false, val query: String = "",
    val batchMode: Boolean = false, val selected: Set<String> = emptySet(),
    val transferMode: Int? = null, val watchLaterVisibleCount: Int = 20,
)
internal data class LibraryDeleteSelection(val mode: Int, val ids: Set<String>)
internal data class LibraryTransferSelection(val moving: Boolean, val ids: Set<String>)

/** 搜索只提交修剪后的关键词；选择与本地显示页数保留 main 的计数规则。 */
internal class LibraryInteractionStateHolder(
    private val settings: GeneralSettings, changes: Flow<Unit>, scope: CoroutineScope,
) {
    private val mutable = MutableStateFlow(LibraryInteractionState(settings.searchHistory))
    val state = mutable.asStateFlow()
    private var closed = false
    private val observer = scope.launch { changes.collect {
        if (!closed) mutable.update { it.copy(searchHistory = settings.searchHistory) }
    } }
    fun close() { closed = true; observer.cancel() }
    fun pageChanged() { update { it.copy(selected = emptySet(), batchMode = false) } }
    fun toggleBatch() { update { it.copy(batchMode = !it.batchMode, selected = emptySet()) } }
    fun toggleSearch() { update { it.copy(searching = !it.searching, query = if (it.searching) "" else it.query) } }
    fun closeSearch() { update { it.copy(searching = false, query = "") } }
    fun dismissTransfer() { update { it.copy(transferMode = null) } }
    fun selectFolder() { update { it.copy(selected = emptySet()) } }
    fun toggleSelected(id: String) { update {        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    } }
    /** 长按面板里的单项操作：只选中这一条，复用批量管理的删除/移动/复制。 */
    fun selectOnly(id: String) { update { it.copy(selected = setOf(id)) } }
    fun back(): Boolean {
        if (closed) return false
        val current = mutable.value
        return when {
            current.transferMode != null -> { dismissTransfer(); false }
            current.searching || current.batchMode -> {
                update { it.copy(searching = false, batchMode = false, selected = emptySet(), query = "") }; false
            }
            else -> true
        }
    }
    fun submitQuery(word: String) {
        if (closed) return
        val query = word.trim()
        if (query.isNotEmpty()) { settings.addSearchHistory(query); update { it.copy(query = query) } }
    }
    fun clearSearchHistory() {
        if (closed) return
        settings.clearSearchHistory(); update { it.copy(query = "") }
    }
    fun resetWatchLaterCount() { update { it.copy(watchLaterVisibleCount = 20) } }
    fun showMoreWatchLater(total: Int) { update { it.copy(watchLaterVisibleCount = (it.watchLaterVisibleCount + 20).coerceAtMost(total)) } }
    fun visibleVideos(items: List<VideoItem>): List<VideoItem> {
        val query = mutable.value.query
        return if (query.isBlank()) items else items.filter {
            it.title.contains(query, ignoreCase = true) || it.ownerName.contains(query, ignoreCase = true)
        }
    }
    fun visibleEntries(items: List<HistoryItem>): List<HistoryItem> {
        val query = mutable.value.query
        return if (query.isBlank()) items else items.filter {
            it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true)
        }
    }
    fun idsOf(mode: Int, data: LibraryUiState): List<String> = when (mode) {
        0 -> visibleVideos(data.favorites.videos).map { it.bvid }
        1 -> visibleEntries(data.history.entries).map { it.bvid }
        else -> visibleEntries(data.watchLater).map { it.bvid }
    }
    fun batchAction(key: String, mode: Int, data: LibraryUiState): LibraryDeleteSelection? {
        if (closed) return null
        when (key) {
            "all" -> {
                val all = idsOf(mode, data)
                update { it.copy(selected = if (all.isNotEmpty() && it.selected.size == all.size) emptySet() else all.toSet()) }
            }
            "delete" -> {
                val ids = mutable.value.selected
                update { it.copy(batchMode = false, selected = emptySet()) }
                return LibraryDeleteSelection(mode, ids)
            }
            "move" -> if (mode == 0) update { it.copy(transferMode = 1) }
            "copy" -> if (mode == 0) update { it.copy(transferMode = 2) }
            // 批量面板上的「取消」：直接退出批量管理，和返回键效果一致。
            "cancel" -> update { it.copy(batchMode = false, selected = emptySet(), transferMode = null) }
        }
        return null
    }
    fun beginTransfer(kind: Int): LibraryTransferSelection? {
        if (closed) return null
        val ids = mutable.value.selected
        update { it.copy(transferMode = null, batchMode = false, selected = emptySet()) }
        return LibraryTransferSelection(kind == 1, ids)
    }
    private fun update(transform: (LibraryInteractionState) -> LibraryInteractionState) {
        if (!closed) mutable.update(transform)
    }
}
