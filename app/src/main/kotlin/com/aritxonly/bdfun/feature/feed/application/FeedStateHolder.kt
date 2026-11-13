package com.aritxonly.bdfun.feature.feed.application

import com.aritxonly.bdfun.core.domain.dynamicFeedTypes
import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.feed.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

internal data class FeedListUiState(
    val type: String = "all", val filterMid: Long? = null, val items: List<FeedItem> = emptyList(),
    val loading: Boolean = false, val refreshing: Boolean = false, val error: String? = null, val hasMore: Boolean = true,
)
internal data class FeedPortalUiState(
    val authors: List<FeedAuthor> = emptyList(), val liveUsers: List<FeedLiveUser> = emptyList(),
    val liveCount: Int = 0, val authorsLoading: Boolean = false, val authorsError: String? = null, val authorsHasMore: Boolean = false,
)
internal data class FeedInteractionUiState(
    val liked: Map<String, Boolean> = emptyMap(), val likeCounts: Map<String, Int> = emptyMap(), val pending: Set<String> = emptySet(),
)
internal data class FeedUiState(
    val feed: FeedListUiState = FeedListUiState(),
    val portal: FeedPortalUiState = FeedPortalUiState(),
    val interactions: FeedInteractionUiState = FeedInteractionUiState(),
)

/** 每个分类和作者独立游标；generation 防止迟到响应覆盖新选择。 */
internal class FeedStateHolder(private val repository: FeedPageRepository) {
    private val mutable = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = mutable.asStateFlow()
    private var closed = false
    fun close() {
        closed = true
        generation++
        portalGeneration++
        mutable.update { it.copy(feed = it.feed.copy(loading = false, refreshing = false),
            portal = it.portal.copy(authorsLoading = false),
            interactions = it.interactions.copy(pending = emptySet())) }
    }
    private var lastMid: Long? = null
    var type: String
        get() = mutable.value.feed.type
        private set(value) { mutable.update { it.copy(feed = it.feed.copy(type = value)) } }
    var filterMid: Long?
        get() = mutable.value.feed.filterMid
        private set(value) { mutable.update { it.copy(feed = it.feed.copy(filterMid = value)) } }
    var items: List<FeedItem>
        get() = mutable.value.feed.items
        private set(value) { mutable.update { it.copy(feed = it.feed.copy(items = value)) } }
    var loading: Boolean
        get() = mutable.value.feed.loading
        private set(value) { mutable.update { it.copy(feed = it.feed.copy(loading = value)) } }
    var refreshing: Boolean
        get() = mutable.value.feed.refreshing
        private set(value) { mutable.update { it.copy(feed = it.feed.copy(refreshing = value)) } }
    var error: String?
        get() = mutable.value.feed.error
        private set(value) { mutable.update { it.copy(feed = it.feed.copy(error = value)) } }
    var hasMore: Boolean
        get() = mutable.value.feed.hasMore
        private set(value) { mutable.update { it.copy(feed = it.feed.copy(hasMore = value)) } }
    var authors: List<FeedAuthor>
        get() = mutable.value.portal.authors
        private set(value) { mutable.update { it.copy(portal = it.portal.copy(authors = value)) } }
    var liveUsers: List<FeedLiveUser>
        get() = mutable.value.portal.liveUsers
        private set(value) { mutable.update { it.copy(portal = it.portal.copy(liveUsers = value)) } }
    var liveCount: Int
        get() = mutable.value.portal.liveCount
        private set(value) { mutable.update { it.copy(portal = it.portal.copy(liveCount = value)) } }
    var authorsLoading: Boolean
        get() = mutable.value.portal.authorsLoading
        private set(value) { mutable.update { it.copy(portal = it.portal.copy(authorsLoading = value)) } }
    var authorsError: String?
        get() = mutable.value.portal.authorsError
        private set(value) { mutable.update { it.copy(portal = it.portal.copy(authorsError = value)) } }
    var authorsHasMore: Boolean
        get() = mutable.value.portal.authorsHasMore
        private set(value) { mutable.update { it.copy(portal = it.portal.copy(authorsHasMore = value)) } }
    val activeLoading get() = loading
    val liked get() = mutable.value.interactions.liked
    val likeCounts get() = mutable.value.interactions.likeCounts

    private var authorsOffset = ""
    private var portalLoaded = false
    private var portalGeneration = 0
    private var generation = 0
    private var offset = ""
    private var started = false
    private var retryReset = true
    private data class CachedPage(val items: List<FeedItem>, val offset: String, val hasMore: Boolean)
    private val cache = mutableMapOf<String, CachedPage>()
    private val key get() = "$type:${filterMid ?: 0}"

    fun markSeen(mid: Long) {
        if (closed) return
        authors = authors.map { if (it.mid == mid) it.copy(hasUpdate = false) else it }
    }

    suspend fun ensureLoaded(loggedIn: Boolean) {
        if (closed || !loggedIn) return
        coroutineScope {
            if (!portalLoaded && !authorsLoading) launch { refreshAuthors() }
            if (!started && !refreshing) fetch(reset = true)
        }
    }

    suspend fun switchTo(next: String, loggedIn: Boolean) {
        if (closed || next !in dynamicFeedTypes) return
        val mid = if (next == "up") lastMid else null
        if (type == next && filterMid == mid) return
        select(next, mid, loggedIn)
    }

    suspend fun clearFilter() {
        if (closed) return
        if (type == "all") refresh(true) else switchTo("all", true)
    }

    suspend fun filterTo(mid: Long) {
        if (closed || mid <= 0) return
        markSeen(mid)
        lastMid = mid
        // Tapping the selected author reloads their feed, as in Hyper-PiliPlus.
        if (type == "up" && filterMid == mid) fetch(reset = true)
        else select("up", mid, true)
    }

    private suspend fun select(next: String, mid: Long?, loggedIn: Boolean) {
        if (closed) return
        generation++
        type = next
        filterMid = mid
        loading = false
        refreshing = false
        error = null
        val saved = cache[key]
        items = saved?.items.orEmpty()
        offset = saved?.offset.orEmpty()
        hasMore = saved?.hasMore ?: true
        started = saved != null
        if (loggedIn && saved == null && (next != "up" || mid != null)) fetch(reset = true)
    }

    suspend fun refresh(loggedIn: Boolean) {
        if (closed || !loggedIn) return
        coroutineScope {
            launch { refreshAuthors() }
            fetch(reset = true)
        }
    }

    suspend fun loadMore(loggedIn: Boolean) {
        if (!closed && loggedIn && !loading && !refreshing && hasMore) fetch(reset = false)
    }

    suspend fun retry(loggedIn: Boolean) {
        if (!closed && loggedIn && !loading && !refreshing) fetch(reset = retryReset)
    }

    private suspend fun fetch(reset: Boolean) {
        if (closed) return
        if (type == "up" && filterMid == null) return
        if (reset && refreshing) return
        val ticket = if (reset) ++generation else generation
        val requestKey = key
        val requestType = type
        val requestMid = filterMid
        if (reset) { refreshing = true; loading = false } else loading = true
        error = null
        retryReset = reset
        try {
            var cursor = if (reset) "" else offset
            val collected = if (reset) mutableListOf() else items.toMutableList()
            val known = collected.mapTo(HashSet()) { it.id }
            val visited = hashSetOf(cursor)
            var more = false
            // Skip duplicate/unsupported-only pages, bounded to avoid a broken server cursor loop.
            for (attempt in 0 until 5) {
                val page = when (val result = repository.dynamicPage(requestType, cursor, requestMid)) {
                    is Outcome.Success -> result.value
                    is Outcome.Failure -> {
                        if (ticket == generation) error = result.error.messageOr("动态加载失败，点击重试")
                        return
                    }
                }
                if (ticket != generation) return
                val fresh = page.items.filter { it.id.isNotBlank() && known.add(it.id) }
                collected.addAll(fresh)
                more = page.hasMore && page.offset.isNotBlank() && visited.add(page.offset)
                cursor = page.offset
                if (fresh.isNotEmpty() || !more) break
            }
            items = collected.toList()
            offset = cursor
            hasMore = more
            started = true // Empty successful feeds must not refetch on every visit.
            cache[requestKey] = CachedPage(collected.toList(), cursor, more)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            if (ticket == generation) error = failure.message?.takeIf { it.isNotBlank() } ?: "动态加载失败，点击重试"
        } finally {
            if (ticket == generation) { refreshing = false; loading = false }
        }
    }

    suspend fun refreshAuthors() {
        if (closed) return
        val ticket = ++portalGeneration
        authorsLoading = true
        authorsError = null
        try {
            val portal = when (val result = repository.dynamicPortal()) {
                is Outcome.Success -> result.value
                is Outcome.Failure -> {
                    if (ticket == portalGeneration) authorsError = "UP 列表加载失败，点击重试"
                    return
                }
            }
            if (ticket != portalGeneration) return
            authors = portal.upList.items.distinctBy { it.mid }
            authorsOffset = portal.upList.offset
            authorsHasMore = portal.upList.hasMore && authorsOffset.isNotBlank()
            liveUsers = portal.liveUsers.distinctBy { it.roomId }
            liveCount = portal.liveCount
            portalLoaded = true
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            if (ticket == portalGeneration) authorsError = "UP 列表加载失败，点击重试"
        } finally {
            if (ticket == portalGeneration) authorsLoading = false
        }
    }

    suspend fun loadMoreAuthors() {
        if (closed || authorsLoading || !authorsHasMore) return
        val ticket = portalGeneration
        val cursor = authorsOffset
        authorsLoading = true
        authorsError = null
        try {
            val page = when (val result = repository.dynamicUpList(cursor)) {
                is Outcome.Success -> result.value
                is Outcome.Failure -> {
                    if (ticket == portalGeneration) authorsError = "UP 列表加载失败，点击重试"
                    return
                }
            }
            if (ticket != portalGeneration) return
            authors = (authors + page.items).distinctBy { it.mid }
            authorsOffset = page.offset
            authorsHasMore = page.hasMore && page.offset.isNotBlank() && page.offset != cursor
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            if (ticket == portalGeneration) authorsError = "UP 列表加载失败，点击重试"
        } finally {
            if (ticket == portalGeneration) authorsLoading = false
        }
    }

    suspend fun toggleLike(item: FeedItem): Outcome<Boolean>? {
        val old = mutable.value.interactions
        if (closed || item.id in old.pending) return null
        val previous = old.liked[item.id] ?: item.liked
        val count = old.likeCounts[item.id] ?: item.like
        val next = !previous
        mutable.update { it.copy(interactions = it.interactions.copy(
            liked = it.interactions.liked + (item.id to next),
            likeCounts = it.interactions.likeCounts + (item.id to (count + if (next) 1 else -1).coerceAtLeast(0)),
            pending = it.interactions.pending + item.id,
        )) }
        fun rollback() { if (closed) return; mutable.update { it.copy(interactions = it.interactions.copy(
            liked = it.interactions.liked + (item.id to previous), likeCounts = it.interactions.likeCounts + (item.id to count),
        )) } }
        return try {
            val result = repository.likeFeed(item.id, next)
            if (closed) return null
            when (result) {
                is Outcome.Success -> if (result.value) Outcome.Success(next) else {
                    rollback(); Outcome.Failure(AppError.Unknown("点赞失败"))
                }
                is Outcome.Failure -> { rollback(); result }
            }
        } catch (cancel: CancellationException) { rollback(); throw cancel }
        finally { if (!closed) mutable.update { it.copy(interactions = it.interactions.copy(pending = it.interactions.pending - item.id)) } }
    }
}
