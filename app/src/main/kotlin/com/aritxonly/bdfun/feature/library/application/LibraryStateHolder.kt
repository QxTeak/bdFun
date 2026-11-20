package com.aritxonly.bdfun.feature.library.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.LibraryRepository
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class FavoriteUiState(
    val folders: List<FavoriteFolder> = emptyList(), val index: Int = 0,
    val videos: List<VideoItem> = emptyList(), val page: Int = 1,
    val hasMore: Boolean = false, val loadingMore: Boolean = false, val ready: Boolean = false,
    val revision: Int = 0,
)
internal data class HistoryUiState(
    val entries: List<HistoryItem> = emptyList(), val max: Long = 0, val viewAt: Long = 0,
    val business: String = "", val hasMore: Boolean = false, val loadingMore: Boolean = false,
    val revision: Int = 0,
)
internal data class LibraryUiState(
    val favorites: FavoriteUiState = FavoriteUiState(), val history: HistoryUiState = HistoryUiState(),
    val watchLater: List<HistoryItem> = emptyList(), val loading: Map<Int, Boolean> = emptyMap(),
    val errors: Map<Int, String?> = emptyMap(), val busy: Boolean = false,
)

/** 收藏夹切换使用版本号隔离，分页保留接口的原始游标与去重行为。 */
internal class LibraryStateHolder(
    private val repository: LibraryRepository, private val currentOwner: () -> Boolean = { true },
) {
    private val mutable = MutableStateFlow(LibraryUiState())
    val state = mutable.asStateFlow()
    private var closed = false
    val isActive get() = !closed && currentOwner()
    fun close() {
        if (closed) return
        closed = true
        mutable.update { it.copy(
            busy = false,
            loading = it.loading.mapValues { false },
            favorites = it.favorites.copy(
                loadingMore = false,
            ),
            history = it.history.copy(
                loadingMore = false,
            ),
        ) }
    }
    private suspend fun <T> request(action: suspend () -> Outcome<T>): Outcome<T> {
        if (!isActive) throw CancellationException("媒体库页面已关闭")
        val result = action()
        currentCoroutineContext().ensureActive()
        if (!isActive) throw CancellationException("媒体库页面已关闭")
        return result
    }
    private fun loading(tab: Int, value: Boolean) = mutable.update { if (!isActive) it else it.copy(loading = it.loading + (tab to value)) }
    private fun error(tab: Int, value: String?) = mutable.update { if (!isActive) it else it.copy(errors = it.errors + (tab to value)) }

    suspend fun loadFavorites() {
        if (!isActive || mutable.value.loading[0] == true || mutable.value.busy) return
        loading(0, true); error(0, null)
        try {
            when (val result = request { repository.folders() }) {
                is Outcome.Success -> {
                    mutable.update { it.copy(
                        favorites = it.favorites.copy(
                            folders = result.value,
                            index = 0,
                        ),
                    ) }
                    if (result.value.isEmpty()) error(0, "还没有收藏夹") else loadFolder(0)
                }
                is Outcome.Failure -> error(0, result.error.messageOr("收藏夹加载失败"))
            }
        } finally { loading(0, false) }
    }

    /** 移动/复制/删除只影响收藏夹数量：只换收藏夹列表，不动当前列表与滚动位置。 */
    suspend fun refreshFolderCounts() {
        if (!isActive) return
        when (val result = request { repository.folders() }) {
            is Outcome.Success -> mutable.update { state ->
                val current = state.favorites
                val currentId = current.folders.getOrNull(current.index)?.id
                val index = result.value.indexOfFirst { it.id == currentId }
                    .takeIf { it >= 0 }
                    ?: current.index.coerceIn(0, (result.value.size - 1).coerceAtLeast(0))
                state.copy(favorites = current.copy(folders = result.value, index = index))
            }
            is Outcome.Failure -> Unit
        }
    }

    suspend fun loadFolder(index: Int) {        if (!isActive || mutable.value.busy) return
        val initial = mutable.value.favorites
        val folder = initial.folders.getOrNull(index) ?: return
        val revision = initial.revision + 1
        mutable.update { it.copy(
            favorites = it.favorites.copy(
                index = index,
                revision = revision,
                loadingMore = false,
            ),
        ) }
        loading(0, true); error(0, null)
        try {
            when (val result = request { repository.videos(folder.id, 1) }) {
                is Outcome.Success -> updateFavorite(revision) { it.copy(videos = result.value.first,
                    page = 1, hasMore = result.value.second, ready = true) }
                is Outcome.Failure -> if (mutable.value.favorites.revision == revision) {
                    error(0, result.error.messageOr("收藏夹加载失败"))
                    updateFavorite(revision) { it.copy(videos = emptyList(), page = 1, hasMore = false, ready = true) }
                }
            }
        } finally { if (mutable.value.favorites.revision == revision) loading(0, false) }
    }

    suspend fun loadMoreFolders() {
        val initial = mutable.value.favorites
        if (!isActive || !initial.hasMore || initial.loadingMore || mutable.value.loading[0] == true || mutable.value.busy) return
        val folder = initial.folders.getOrNull(initial.index) ?: return
        updateFavorite(initial.revision) { it.copy(loadingMore = true) }
        try {
            when (val result = request { repository.videos(folder.id, initial.page + 1) }) {
                is Outcome.Success -> updateFavorite(initial.revision) { it.copy(
                    videos = (it.videos + result.value.first).distinctBy { video -> video.bvid },
                    page = initial.page + 1, hasMore = result.value.second,
                ) }
                is Outcome.Failure -> if (mutable.value.favorites.revision == initial.revision)
                    error(0, result.error.messageOr("收藏夹加载失败"))
            }
        } finally { updateFavorite(initial.revision) { it.copy(loadingMore = false) } }
    }

    suspend fun loadHistory() {
        if (!isActive || mutable.value.loading[1] == true || mutable.value.busy) return
        val revision = mutable.value.history.revision + 1
        mutable.update { it.copy(history = it.history.copy(revision = revision, loadingMore = false)) }
        loading(1, true); error(1, null)
        try {
            when (val result = request { repository.history(0, 0, "") }) {
                is Outcome.Success -> {
                    val page = result.value
                    val entries = enrich(page.items)
                    updateHistory(revision) { it.copy(entries = entries, max = page.max, viewAt = page.viewAt,
                        business = page.business, hasMore = entries.isNotEmpty()) }
                }
                is Outcome.Failure -> error(1, result.error.messageOr("历史记录加载失败"))
            }
        } finally { if (mutable.value.history.revision == revision) loading(1, false) }
    }

    suspend fun loadMoreHistory() {
        val initial = mutable.value.history
        if (!isActive || !initial.hasMore || initial.loadingMore || mutable.value.loading[1] == true || mutable.value.busy) return
        updateHistory(initial.revision) { it.copy(loadingMore = true) }
        try {
            when (val result = request { repository.history(initial.max, initial.viewAt, initial.business) }) {
                is Outcome.Success -> {
                    val page = result.value
                    val enriched = enrich(page.items)
                    updateHistory(initial.revision) {
                        val existing = it.entries.mapTo(HashSet()) { entry -> entry.bvid }
                        val added = enriched.filter { entry -> existing.add(entry.bvid) }
                        it.copy(entries = it.entries + added, max = page.max, viewAt = page.viewAt,
                            business = page.business, hasMore = added.isNotEmpty())
                    }
                }
                is Outcome.Failure -> if (mutable.value.history.revision == initial.revision)
                    error(1, result.error.messageOr("历史记录加载失败"))
            }
        } finally { updateHistory(initial.revision) { it.copy(loadingMore = false) } }
    }

    suspend fun loadWatchLater() {
        if (!isActive || mutable.value.loading[2] == true || mutable.value.busy) return
        loading(2, true); error(2, null)
        try {
            when (val result = request { repository.watchLater() }) {
                is Outcome.Success -> mutable.update { it.copy(watchLater = result.value) }
                is Outcome.Failure -> error(2, result.error.messageOr("稍后再看加载失败"))
            }
        } finally { loading(2, false) }
    }

    private suspend fun enrich(items: List<HistoryItem>): List<HistoryItem> = if (items.isEmpty()) items else
        when (val result = request { repository.enrich(items) }) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> items
        }

    private fun updateFavorite(revision: Int, transform: (FavoriteUiState) -> FavoriteUiState) = mutable.update {
        if (isActive && it.favorites.revision == revision) it.copy(
            favorites = transform(it.favorites),
        ) else it
    }
    private fun updateHistory(revision: Int, transform: (HistoryUiState) -> HistoryUiState) = mutable.update {
        if (isActive && it.history.revision == revision) it.copy(history = transform(it.history)) else it
    }

    suspend fun deleteSelected(mode: Int, ids: Set<String>): String? {
        if (!isActive || ids.isEmpty() || mutable.value.busy || mutable.value.loading[mode] == true) return null
        mutable.update { it.copy(busy = true) }
        try {
            val snapshot = mutable.value
            val successful = mutableSetOf<String>()
            val targets = when (mode) {
                0 -> snapshot.favorites.videos.filter { it.bvid in ids }.map { it.bvid to it.aid }
                1 -> snapshot.history.entries.filter { it.bvid in ids }.map { it.bvid to it.aid }
                else -> snapshot.watchLater.filter { it.bvid in ids }.map { it.bvid to it.aid }
            }
            val folder = snapshot.favorites.folders.getOrNull(snapshot.favorites.index)
            for ((bvid, aid) in targets) {
                val result = when {
                    aid <= 0 -> null
                    mode == 0 && folder != null -> request { repository.removeFavorite(aid, folder.id) }
                    mode == 1 -> request { repository.deleteHistory(aid) }
                    mode == 2 -> request { repository.deleteWatchLater(aid) }
                    else -> null
                }
                if (result is Outcome.Success && result.value) {
                    successful += bvid
                    remove(mode, setOf(bvid))
                }
            }
            return if (successful.size == ids.size) "已删除 ${ids.size} 项" else "部分删除失败，请重试"
        } finally { mutable.update { it.copy(busy = false) } }
    }

    suspend fun transferSelected(moving: Boolean, target: FavoriteFolder, ids: Set<String>): String? {
        if (!isActive || ids.isEmpty() || mutable.value.busy || mutable.value.loading[0] == true) return null
        val snapshot = mutable.value.favorites
        val source = snapshot.folders.getOrNull(snapshot.index) ?: return null
        // 移动到原收藏夹不应把刚添加成功的视频再删除。
        if (moving && target.id == source.id) return "已在「${target.title}」中"
        mutable.update { it.copy(busy = true) }
        var allOk = true
        try {
            for (video in snapshot.videos.filter { it.bvid in ids }) {
                val added = request { repository.addFavorite(video.aid, target.id) }
                if (added !is Outcome.Success || !added.value) { allOk = false; continue }
                if (moving) {
                    val removed = request { repository.removeFavorite(video.aid, source.id) }
                    if (removed is Outcome.Success && removed.value) remove(0, setOf(video.bvid)) else allOk = false
                }
            }
            return if (!allOk) "部分操作失败，请重试" else if (moving) "已移动到「${target.title}」" else "已复制到「${target.title}」"
        } finally { mutable.update { it.copy(busy = false) } }
    }

    private fun remove(mode: Int, ids: Set<String>) = mutable.update { if (!isActive) it else when (mode) {
        0 -> it.copy(
            favorites = it.favorites.copy(
                videos = it.favorites.videos.filterNot { video -> video.bvid in ids },
            ),
        )
        1 -> it.copy(history = it.history.copy(entries = it.history.entries.filterNot { entry -> entry.bvid in ids }))
        else -> it.copy(watchLater = it.watchLater.filterNot { entry -> entry.bvid in ids })
    } }
}
