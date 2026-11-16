package com.aritxonly.bdfun.feature.home.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.application.StateHolder
import com.aritxonly.bdfun.core.application.UiState
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

internal data class HomeFeedUiState(
    val videos: List<VideoItem> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: AppError? = null,
    val loadMoreError: AppError? = null,
    val endReached: Boolean = false,
    val refreshRequest: Int = 0,
    val scrollRevision: Long = 0,
    val filteredPage: Boolean = false,
    val empty: Boolean = false,
) : UiState

/** 这是首页状态模板：不可变 UiState + StateFlow，界面事件只调用这里的动作。 */
internal class HomeFeedStateHolder(private val feed: FeedRepository, private val videos: VideoRepository,
    private val cache: HomeFeedCache? = null, private val requestTimeoutMs: Long = 12_000,
) : StateHolder<HomeFeedUiState> {
    private val mutable = MutableStateFlow(HomeFeedUiState())
    override val state: StateFlow<HomeFeedUiState> = mutable.asStateFlow()
    private val firstLoad = Mutex()
    private val exclusions = RecommendationExclusions()
    private var page = 1
    private var started = false
    private var generation = 0L
    private var closed = false
    fun close() {
        if (closed) return
        closed = true
        generation++
        mutable.update { it.copy(loading = false, refreshing = false, loadingMore = false) }
    }
    private data class Batch(val items: List<VideoItem>, val rawEmpty: Boolean)

    fun requestRefresh() { if (closed) return; mutable.update { it.copy(refreshRequest = it.refreshRequest + 1) } }
    fun dismissRecommendation(video: VideoItem, creator: Boolean = false, category: String? = null) {
        if (closed) return
        exclusions.exclude(video, creator, category)
        mutable.update { it.copy(videos = it.videos.filter(exclusions::accepts)) }
    }

    suspend fun ensureLoaded() = firstLoad.withLock {
        if (closed || started) return@withLock
        val ticket = ++generation
        mutable.update { it.copy(loading = true, error = null, loadMoreError = null, empty = false) }
        try {
            if (mutable.value.videos.isEmpty()) {
                val saved = (cache?.read() as? Outcome.Success)?.value.orEmpty().filter(exclusions::accepts)
                if (ticket == generation && saved.isNotEmpty()) mutable.update { it.copy(videos = saved, loading = false) }
            }
            when (val result = recommendations(1)) {
                is Outcome.Failure -> if (ticket == generation) mutable.update { it.copy(error = result.error) }
                is Outcome.Success -> if (ticket == generation) {
                    val distinct = result.value.items.distinctBy { it.bvid }
                    if (distinct.isNotEmpty()) {
                        val old = mutable.value.videos
                        val same = old.size == distinct.size && old.indices.all { old[it].bvid == distinct[it].bvid }
                        page = 1
                        started = true
                        mutable.update { it.copy(videos = if (same) old else distinct, endReached = false) }
                        cache?.write(distinct)
                    } else mutable.update { it.copy(empty = it.videos.isEmpty()) }
                }
            }
        } finally {
            if (ticket == generation) mutable.update { it.copy(loading = false, refreshing = false) }
        }
    }

    suspend fun refresh() {
        if (closed || mutable.value.refreshing) return
        val ticket = ++generation
        mutable.update { it.copy(refreshing = true, loadingMore = false, error = null, loadMoreError = null, filteredPage = false, empty = false) }
        try {
            when (val result = recommendations(1)) {
                is Outcome.Failure -> if (ticket == generation) mutable.update { it.copy(error = result.error) }
                is Outcome.Success -> if (ticket == generation) {
                    val items = result.value.items
                    if (items.isNotEmpty()) {
                        started = true
                        mutable.update { old ->
                            val known = old.videos.mapTo(HashSet()) { it.bvid }
                            val fresh = items.filter { known.add(it.bvid) }
                            old.copy(videos = fresh + old.videos, endReached = if (fresh.isNotEmpty()) false else old.endReached,
                                scrollRevision = old.scrollRevision + 1)
                        }
                        cache?.write(items)
                    } else mutable.update { it.copy(empty = it.videos.isEmpty()) }
                }
            }
        } finally {
            if (ticket == generation) mutable.update { it.copy(refreshing = false) }
        }
    }

    suspend fun loadMore() {
        val current = mutable.value
        if (closed || current.loadingMore || current.endReached || current.loading || current.refreshing) return
        val ticket = generation
        val next = page + 1
        mutable.update { it.copy(loadingMore = true, loadMoreError = null, filteredPage = false) }
        try {
            when (val result = recommendations(next)) {
                is Outcome.Failure -> if (ticket == generation) mutable.update { it.copy(loadMoreError = result.error) }
                is Outcome.Success -> if (ticket == generation) {
                    page = next
                    mutable.update { old ->
                        val known = old.videos.mapTo(HashSet()) { it.bvid }
                        val fresh = result.value.items.filter { known.add(it.bvid) }
                        val ended = result.value.rawEmpty || (fresh.isEmpty() && exclusions.isEmpty)
                        old.copy(videos = old.videos + fresh, endReached = ended, filteredPage = fresh.isEmpty() && !ended)
                    }
                }
            }
        } finally {
            if (ticket == generation) mutable.update { it.copy(loadingMore = false) }
        }
    }

    private suspend fun recommendations(page: Int): Outcome<Batch> =
        kotlinx.coroutines.withTimeoutOrNull(requestTimeoutMs) { loadRecommendations(page) }
            ?: Outcome.Failure(AppError.Network("网络较慢，请稍后重试"))

    private suspend fun loadRecommendations(page: Int): Outcome<Batch> {
        val items = when (val result = feed.recommend(page)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        val filtered = coroutineScope {
            val limit = Semaphore(3)
            items.map { video -> async {
                if (exclusions.hasCategories && video.categoryName.isBlank()) {
                    limit.withPermit {
                        when (val detail = videos.videoDetail(video.bvid)) {
                            is Outcome.Success -> video.copy(
                                extras = video.extras.copy(
                                    categoryName = detail.value.tname,
                                ),
                            )
                            is Outcome.Failure -> video
                        }
                    }
                } else video
            } }.map { it.await() }.filter(exclusions::accepts)
        }
        return Outcome.Success(Batch(filtered, items.isEmpty()))
    }
}
