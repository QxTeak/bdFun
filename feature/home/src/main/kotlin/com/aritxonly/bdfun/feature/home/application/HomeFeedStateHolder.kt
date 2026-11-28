package com.aritxonly.bdfun.feature.home.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.application.StateHolder
import com.aritxonly.bdfun.core.application.UiState
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    private val optimizer: com.aritxonly.bdfun.core.application.RecommendationOptimizer? = null,
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
    private var resetOnRefresh = false
    fun modeChanged() {
        if (closed) return
        generation++
        resetOnRefresh = true
        mutable.update { it.copy(loading = false, refreshing = false, loadingMore = false, endReached = false) }
        if (mutable.value.videos.isEmpty()) requestRefresh()
    }
    fun expose(video: VideoItem, ticket: com.aritxonly.bdfun.core.application.RecommendationTicket) {
        if (!closed) optimizer?.expose(video, ticket)
    }
    fun open(video: VideoItem) { if (!closed) optimizer?.open(video) }
    fun exposureTicket() = optimizer?.ticket()
    private fun current(ticket: Long, mode: com.aritxonly.bdfun.core.application.RecommendationTicket?) =
        ticket == generation && (mode == null || optimizer?.valid(mode) == true)

    fun close() {
        if (closed) return
        closed = true
        generation++
        mutable.update { it.copy(loading = false, refreshing = false, loadingMore = false) }
    }
    private data class Batch(val items: List<VideoItem>, val rawEmpty: Boolean)

    fun requestRefresh() { if (closed) return; mutable.update { it.copy(refreshRequest = it.refreshRequest + 1) } }

    /** 反馈成功先只标记模糊，给用户一个可撤销的窗口；真正过滤交给 [commitDismissed]。 */
    private data class PendingDismiss(
        val video: VideoItem, val creator: Boolean, val category: String?,
        /** 撤销时同步撤回服务端反馈；无法撤回的本地动作可以为空。 */
        val undo: (suspend () -> Outcome<Unit>)? = null,
    )

    /** 可以同时有多张待撤销卡：每张卡各自保留自己的反馈和撤销动作。 */
    private val pendingDismisses = mutableMapOf<String, PendingDismiss>()
    fun dismissRecommendation(
        video: VideoItem, creator: Boolean = false, category: String? = null,
        undo: (suspend () -> Outcome<Unit>)? = null,
    ) {
        if (closed) return
        pendingDismisses[video.bvid] = PendingDismiss(video, creator, category, undo)
        mutable.update { it.copy(videos = it.videos.filter(exclusions::accepts)) }
    }

    /** 撤销条超时：按原规则把卡从推荐流里排除。 */
    fun commitDismissed() {
        if (closed || pendingDismisses.isEmpty()) return
        val pending = pendingDismisses.values.toList()
        pendingDismisses.clear()
        pending.forEach { exclusions.exclude(it.video, it.creator, it.category) }
        mutable.update { it.copy(videos = it.videos.filter(exclusions::accepts)) }
    }

    /**
     * 撤销指定卡：先撤回服务端反馈，成功后才算真正恢复；失败保留 pending，用户可重试。
     * 没有服务端动作的本地过滤（如分类本地隐藏）直接恢复。
     */
    suspend fun undoDismissed(bvid: String): Outcome<Unit> {
        if (closed) return Outcome.Success(Unit)
        val pending = pendingDismisses[bvid] ?: return Outcome.Success(Unit)
        val undo = pending.undo
        if (undo == null) {
            pendingDismisses.remove(bvid)
            return Outcome.Success(Unit)
        }
        return try {
            when (val result = undo()) {
                is Outcome.Success -> {
                    pendingDismisses.remove(bvid)
                    result
                }
                is Outcome.Failure -> result
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Outcome.Failure(AppError.Unknown(error.message ?: "撤销失败，请重试"))
        }
    }

    suspend fun ensureLoaded() = firstLoad.withLock {
        if (closed || started) return@withLock
        val mode = optimizer?.ticket()
        val ticket = ++generation
        mutable.update { it.copy(loading = true, error = null, loadMoreError = null, empty = false) }
        try {
            if (mutable.value.videos.isEmpty()) {
                val saved = (cache?.read() as? Outcome.Success)?.value.orEmpty().filter(exclusions::accepts)
                if (current(ticket, mode) && saved.isNotEmpty()) mutable.update { it.copy(videos = saved, loading = false) }
            }
            when (val result = recommendations(1)) {
                is Outcome.Failure -> if (current(ticket, mode)) mutable.update { it.copy(error = result.error) }
                is Outcome.Success -> if (current(ticket, mode)) {
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
            if (current(ticket, mode)) mutable.update { it.copy(loading = false, refreshing = false) }
        }
    }

    suspend fun refresh() {
        if (closed || mutable.value.refreshing) return
        val mode = optimizer?.ticket()
        val ticket = ++generation
        mutable.update { it.copy(refreshing = true, loadingMore = false, error = null, loadMoreError = null, filteredPage = false, empty = false) }
        try {
            when (val result = recommendations(1)) {
                is Outcome.Failure -> if (current(ticket, mode)) mutable.update { it.copy(error = result.error) }
                is Outcome.Success -> if (current(ticket, mode)) {
                    val items = result.value.items
                    if (items.isNotEmpty()) {
                        started = true
                        mutable.update { old ->
                            if (resetOnRefresh) page = 1
                            val retained = if (resetOnRefresh) emptyList() else old.videos
                            val known = retained.mapTo(HashSet()) { it.bvid }
                            val fresh = items.filter { known.add(it.bvid) }
                            resetOnRefresh = false
                            old.copy(videos = if (fresh.isEmpty()) retained else fresh + retained,
                                endReached = if (fresh.isNotEmpty()) false else old.endReached,
                                scrollRevision = old.scrollRevision + 1)
                        }
                        cache?.write(items)
                    } else mutable.update { it.copy(empty = it.videos.isEmpty()) }
                }
            }
        } finally {
            if (current(ticket, mode)) mutable.update { it.copy(refreshing = false) }
        }
    }

    suspend fun loadMore() {
        val current = mutable.value
        if (closed || current.loadingMore || current.endReached || current.loading || current.refreshing) return
        val mode = optimizer?.ticket()
        val ticket = generation
        var next = page + 1
        mutable.update { it.copy(loadingMore = true, loadMoreError = null, filteredPage = false) }
        try {
            var result = recommendations(next)
            var extra = 0
            val initialVideos = mutable.value.videos
            val initialIds by lazy(LazyThreadSafetyMode.NONE) {
                initialVideos.mapTo(HashSet()) { it.bvid }
            }
            while (extra < 2 && optimizer?.state?.value?.enabled == true && current(ticket, mode) &&
                result is Outcome.Success && result.value.items.none { it.bvid !in initialIds }) {
                extra++
                next++
                result = recommendations(next)
            }
            when (result) {
                is Outcome.Failure -> if (current(ticket, mode)) mutable.update { it.copy(loadMoreError = result.error) }
                is Outcome.Success -> if (current(ticket, mode)) {
                    page = next
                    mutable.update { old ->
                        val fresh = if (result.value.items.isEmpty()) emptyList() else {
                            // 额外请求期间可能有卡片被移除；提交时以最新列表为准。
                            val known = if (old.videos === initialVideos) initialIds
                                else old.videos.mapTo(HashSet()) { it.bvid }
                            val freshIds = HashSet<String>()
                            result.value.items.filter { it.bvid !in known && freshIds.add(it.bvid) }
                        }
                        val ended = optimizer?.state?.value?.enabled != true &&
                            (result.value.rawEmpty || (fresh.isEmpty() && exclusions.isEmpty))
                        old.copy(videos = if (fresh.isEmpty()) old.videos else old.videos + fresh,
                            endReached = ended, filteredPage = fresh.isEmpty() && !ended)
                    }
                }
            }
        } finally {
            if (current(ticket, mode)) mutable.update { it.copy(loadingMore = false) }
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
        currentCoroutineContext().ensureActive()
        val candidates = if (exclusions.hasCategories && items.any { it.categoryName.isBlank() }) {
            resolveMissingCategories(items)
        } else items
        val filtered = candidates.filter(exclusions::accepts)
        val mode = optimizer?.ticket()
        val ranked = if (mode == null) filtered else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            optimizer.rank(filtered, mutable.value.videos, mode)
        }
        return Outcome.Success(Batch(ranked, items.isEmpty()))
    }

    private suspend fun resolveMissingCategories(items: List<VideoItem>): List<VideoItem> = coroutineScope {
        val limit = Semaphore(3)
        // 只有缺分类的卡片需要并发请求详情，按原下标等待以保留推荐顺序。
        val pending = items.map { video ->
            if (video.categoryName.isNotBlank()) null else async {
                limit.withPermit {
                    when (val detail = videos.videoDetail(video.bvid)) {
                        is Outcome.Success -> video.copy(extras = video.extras.copy(categoryName = detail.value.tname))
                        is Outcome.Failure -> video
                    }
                }
            }
        }
        items.mapIndexed { index, video -> pending[index]?.await() ?: video }
    }
}
