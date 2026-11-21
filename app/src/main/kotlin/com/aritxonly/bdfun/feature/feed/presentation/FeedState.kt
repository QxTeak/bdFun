package com.aritxonly.bdfun.feature.feed.presentation

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.*
import com.aritxonly.bdfun.feature.feed.application.FeedStateHolder
import com.aritxonly.bdfun.feature.feed.application.FeedUiState
import com.aritxonly.bdfun.core.model.feed.FeedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 页面只观察不可变状态；手势和布局对象留在表现层。 */
@Stable
class FeedState internal constructor(
    private val controller: FeedStateHolder,
    scope: CoroutineScope,
    /** 非「全部」分类共享「全部」的 UP/直播 portal，不再各自请求一次。 */
    private val portalSource: StateFlow<FeedUiState>? = null,
) {
    private var observed by mutableStateOf(controller.state.value)
    private var portalObserved by mutableStateOf(controller.state.value.portal)
    val listState = LazyStaggeredGridState()
    var pullFraction by mutableFloatStateOf(0f)
    var pullOffsetDp by mutableFloatStateOf(0f)
    var showLive by mutableStateOf(true)
    init {
        scope.launch {
            controller.state.collect {
                observed = it
                if (portalSource == null) portalObserved = it.portal
            }
        }
        if (portalSource != null) {
            scope.launch { portalSource.collect { portalObserved = it.portal } }
        }
    }
    val type get() = observed.feed.type
    val filterMid get() = observed.feed.filterMid
    val items get() = observed.feed.items
    val loading get() = observed.feed.loading
    val refreshing get() = observed.feed.refreshing
    val error get() = observed.feed.error
    val hasMore get() = observed.feed.hasMore
    val authors get() = portalObserved.authors
    val liveUsers get() = portalObserved.liveUsers
    val liveCount get() = portalObserved.liveCount
    val authorsLoading get() = portalObserved.authorsLoading
    val authorsError get() = portalObserved.authorsError
    val authorsHasMore get() = portalObserved.authorsHasMore
    val activeLoading get() = loading
    val liked get() = observed.interactions.liked
    val likeCounts get() = observed.interactions.likeCounts

    /** 供其它分类订阅共享 portal。 */
    internal val sourceState: StateFlow<FeedUiState> get() = controller.state

    internal fun close() = controller.close()
    fun markSeen(mid: Long) = controller.markSeen(mid)
    suspend fun ensureLoaded(loggedIn: Boolean) = controller.ensureLoaded(loggedIn)
    internal suspend fun ensurePortalLoaded(loggedIn: Boolean) = controller.ensurePortalLoaded(loggedIn)
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
