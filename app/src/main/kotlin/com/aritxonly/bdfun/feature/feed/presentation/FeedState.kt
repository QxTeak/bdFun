package com.aritxonly.bdfun.feature.feed.presentation

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.*
import com.aritxonly.bdfun.feature.feed.application.FeedStateHolder
import com.aritxonly.bdfun.core.model.feed.FeedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 页面只观察不可变状态；手势和布局对象留在表现层。 */
@Stable
class FeedState internal constructor(private val controller: FeedStateHolder, scope: CoroutineScope) {
    private var observed by mutableStateOf(controller.state.value)
    val listState = LazyStaggeredGridState()
    var pullFraction by mutableFloatStateOf(0f)
    var pullOffsetDp by mutableFloatStateOf(0f)
    var showLive by mutableStateOf(true)
    init { scope.launch { controller.state.collect { observed = it } } }
    val type get() = observed.feed.type
    val filterMid get() = observed.feed.filterMid
    val items get() = observed.feed.items
    val loading get() = observed.feed.loading
    val refreshing get() = observed.feed.refreshing
    val error get() = observed.feed.error
    val hasMore get() = observed.feed.hasMore
    val authors get() = observed.portal.authors
    val liveUsers get() = observed.portal.liveUsers
    val liveCount get() = observed.portal.liveCount
    val authorsLoading get() = observed.portal.authorsLoading
    val authorsError get() = observed.portal.authorsError
    val authorsHasMore get() = observed.portal.authorsHasMore
    val activeLoading get() = loading
    val liked get() = observed.interactions.liked
    val likeCounts get() = observed.interactions.likeCounts
    internal fun close() = controller.close()
    fun markSeen(mid: Long) = controller.markSeen(mid)
    suspend fun ensureLoaded(loggedIn: Boolean) = controller.ensureLoaded(loggedIn)
    suspend fun switchTo(next: String, loggedIn: Boolean) = controller.switchTo(next, loggedIn)
    suspend fun clearFilter() = controller.clearFilter()
    suspend fun filterTo(mid: Long) = controller.filterTo(mid)
    suspend fun refresh(loggedIn: Boolean) = controller.refresh(loggedIn)
    suspend fun loadMore(loggedIn: Boolean) = controller.loadMore(loggedIn)
    suspend fun retry(loggedIn: Boolean) = controller.retry(loggedIn)
    suspend fun refreshAuthors() = controller.refreshAuthors()
    suspend fun loadMoreAuthors() = controller.loadMoreAuthors()
    suspend fun toggleLike(item: FeedItem) = controller.toggleLike(item)
}
