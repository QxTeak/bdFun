package com.aritxonly.bdfun.feature.space.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.SpaceRepository
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class SpaceToolsState(
    val query: String = "", val videos: List<VideoItem> = emptyList(),
    val page: Int = 0, val end: Boolean = false, val loading: Boolean = false,
    val blockBusy: Boolean = false, val blocked: Boolean = false, val message: String? = null,
)

internal class SpaceToolsStateHolder(
    private val mid: Long, private val repository: SpaceRepository,
    private val currentOwner: () -> Boolean,
) {
    val dynamics = SpaceDynamicSearchStateHolder(mid, repository, currentOwner)
    private val mutable = MutableStateFlow(SpaceToolsState())
    val state = mutable.asStateFlow()
    private var revision = 0
    private var closed = false
    private val active get() = !closed && currentOwner()
    fun close() { closed = true; revision++; dynamics.close() }
    fun query(value: String) {
        if (!active) return
        revision++
        mutable.update { it.copy(query = value, videos = emptyList(), page = 0, end = false, loading = false, message = null) }
    }
    suspend fun search() {
        val initial = mutable.value
        if (!active || initial.loading || initial.end || initial.query.isBlank()) return
        val version = revision
        mutable.update { it.copy(loading = true, message = null) }
        try {
            val result = repository.searchVideos(mid, initial.query.trim(), initial.page + 1)
            currentCoroutineContext().ensureActive()
            if (!active || version != revision) return
            mutable.update { when (result) {
                is Outcome.Success -> it.copy(videos = (it.videos + result.value).distinctBy { v -> v.bvid },
                    page = initial.page + 1, end = result.value.size < 30)
                is Outcome.Failure -> it.copy(message = result.error.messageOr("搜索失败"))
            } }
        } finally {
            if (active && version == revision) mutable.update { it.copy(loading = false) }
        }
    }
    suspend fun block() {
        if (!active || mutable.value.blockBusy || mutable.value.blocked) return
        mutable.update { it.copy(blockBusy = true, message = null) }
        try {
            val result = repository.block(mid)
            currentCoroutineContext().ensureActive()
            if (!active) return
            mutable.update { when (result) {
                is Outcome.Success -> it.copy(blocked = result.value, message = if (result.value) "已拉黑" else "拉黑失败")
                is Outcome.Failure -> it.copy(message = result.error.messageOr("拉黑失败"))
            } }
        } finally { if (active) mutable.update { it.copy(blockBusy = false) } }
    }
}

internal fun filterSpaceDynamics(items: List<FeedItem>, query: String): List<FeedItem> {
    val keyword = query.trim()
    fun matches(item: FeedItem): Boolean = listOf(item.text, item.title, item.video?.title.orEmpty(), item.pgcTitle)
        .any { it.contains(keyword, ignoreCase = true) } || item.orig?.let(::matches) == true
    return if (keyword.isEmpty()) items else items.filter(::matches)
}
