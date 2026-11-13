package com.aritxonly.bdfun.feature.mine.application

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.LibraryRepository
import com.aritxonly.bdfun.core.model.library.FavoriteFolder
import com.aritxonly.bdfun.core.model.library.HistoryItem
import com.aritxonly.bdfun.core.model.settings.MineLayout
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.model.video.VideoItemExtras
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val MineVideoPreviewLimit = 5
internal const val MineFolderPreviewLimit = 4

internal enum class MinePreviewKind { History, Favorites, WatchLater }
internal data class MinePreviewSection(
    val loading: Boolean = false, val loaded: Boolean = false, val error: AppError? = null,
    val entries: List<HistoryItem> = emptyList(), val folders: List<FavoriteFolder> = emptyList(),
)
internal data class MinePreviewUiState(val sections: Map<MinePreviewKind, MinePreviewSection> = emptyMap()) {
    operator fun get(kind: MinePreviewKind) = sections[kind] ?: MinePreviewSection()
}

/** 每轮配置拥有独立代数；取消或账号失效后不能把旧列表发布给新页面。 */
internal class MinePreviewStateHolder(
    private val repository: LibraryRepository, private val currentOwner: () -> Boolean,
) {
    private val mutable = MutableStateFlow(MinePreviewUiState())
    val state = mutable.asStateFlow()
    private var closed = false
    private var generation = 0
    fun close() { closed = true; generation++; mutable.value = MinePreviewUiState() }
    suspend fun load(layout: MineLayout) = coroutineScope {
        val token = ++generation
        if (!active(token)) { mutable.value = MinePreviewUiState(); return@coroutineScope }
        val enabled = buildSet {
            if (layout.history) add(MinePreviewKind.History)
            if (layout.favorites) add(MinePreviewKind.Favorites)
            if (layout.watchLater) add(MinePreviewKind.WatchLater)
        }
        // 前台刷新保留已展示的内容，只移除被用户关闭的区块；新代数不继承旧请求的忙碌锁。
        mutable.value = MinePreviewUiState(mutable.value.sections.filterKeys { it in enabled }
            .mapValues { (_, section) -> section.copy(loading = false) })
        if (layout.history) launch { loadSection(MinePreviewKind.History, token) }
        if (layout.favorites) launch { loadSection(MinePreviewKind.Favorites, token) }
        if (layout.watchLater) launch { loadSection(MinePreviewKind.WatchLater, token) }
    }
    suspend fun retry(kind: MinePreviewKind) = loadSection(kind, generation)
    private fun active(token: Int) = !closed && currentOwner() && token == generation
    private fun publish(kind: MinePreviewKind, token: Int, section: MinePreviewSection) {
        if (active(token)) mutable.value = MinePreviewUiState(mutable.value.sections + (kind to section))
    }
    private suspend fun loadSection(kind: MinePreviewKind, token: Int) {
        if (!active(token) || state.value[kind].loading) return
        val previous = state.value[kind]
        publish(kind, token, previous.copy(loading = true, error = null))
        try {
            val result = fetch(kind, token)
            currentCoroutineContext().ensureActive()
            when (result) {
                is Outcome.Success -> publish(kind, token, result.value.copy(loaded = true))
                is Outcome.Failure -> publish(kind, token, previous.copy(loading = false, error = result.error))
            }
        } finally {
            if (active(token)) publish(kind, token, state.value[kind].copy(loading = false))
        }
    }
    private suspend fun fetch(kind: MinePreviewKind, token: Int): Outcome<MinePreviewSection> = when (kind) {
        MinePreviewKind.Favorites -> favoritePreviews(token)
        MinePreviewKind.History -> when (val result = repository.history(0, 0, "")) {
            is Outcome.Success -> Outcome.Success(MinePreviewSection(entries = result.value.items.sortedByDescending { it.viewedAt }.take(MineVideoPreviewLimit)))
            is Outcome.Failure -> result
        }
        MinePreviewKind.WatchLater -> when (val result = repository.watchLater()) {
            is Outcome.Success -> Outcome.Success(MinePreviewSection(entries = result.value.sortedByDescending { it.viewedAt }.take(MineVideoPreviewLimit)))
            is Outcome.Failure -> result
        }
    }
    private suspend fun favoritePreviews(token: Int): Outcome<MinePreviewSection> = coroutineScope {
        checkOwner(token)
        val result = repository.folders()
        checkOwner(token)
        when (result) {
            is Outcome.Failure -> result
            is Outcome.Success -> {
                val cachedCovers = state.value[MinePreviewKind.Favorites].folders.associate { it.id to it.cover }
                val folders = result.value.take(MineFolderPreviewLimit).map { folder -> async {
                    checkOwner(token)
                    if (folder.count <= 0) return@async folder.copy(cover = "")
                    // 仓储第一页沿用接口的 mtime 顺序，不能按视频发布时间重排收藏封面。
                    val videos = repository.videos(folder.id, 1)
                    checkOwner(token)
                    val cover = when (videos) {
                        is Outcome.Success -> videos.value.first.firstOrNull()?.cover.orEmpty()
                        is Outcome.Failure -> cachedCovers[folder.id].orEmpty()
                    }
                    folder.copy(cover = cover)
                } }.awaitAll()
                Outcome.Success(MinePreviewSection(folders = folders))
            }
        }
    }
    private suspend fun checkOwner(token: Int) {
        currentCoroutineContext().ensureActive()
        if (!active(token)) throw CancellationException("我的页预览已失效")
    }

}

internal fun HistoryItem.mineVideo(resume: Boolean): VideoItem = VideoItem(
    bvid = bvid, aid = aid, title = title, cover = cover, ownerName = author,
    ownerFace = "", ownerMid = 0, play = play.toLong(), danmaku = danmaku.toLong(),
    duration = duration, pubdate = viewedAt,
    extras = VideoItemExtras(startPositionMs = if (resume && progress > 0) progress.toLong() * 1000 else 0),
)
