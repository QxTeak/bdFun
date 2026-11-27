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
    // 三个分组各自一个快照状态：点赞只碰 interactions，不会让读 items 的列表作用域一起重组。
    private var feedState by mutableStateOf(controller.state.value.feed)
    private var portalState by mutableStateOf(controller.state.value.portal)
    private var interactionState by mutableStateOf(controller.state.value.interactions)
    val listState = LazyStaggeredGridState()
    var pullFraction by mutableFloatStateOf(0f)
    var pullOffsetDp by mutableFloatStateOf(0f)
    var showLive by mutableStateOf(true)
    /** 卡片省略号/长按共用的形变菜单：每张卡有自己的锚点，当前打开的那条放在这里。 */
    var menuState by mutableStateOf<com.aritxonly.teak.common.navigation.TeakMorphMenuState?>(null)
    var menuItem by mutableStateOf<FeedItem?>(null)
    /** 举报弹窗的目标：菜单收起后由页面继续持有，弹窗独立于菜单宿主。 */
    var reportItem by mutableStateOf<FeedItem?>(null)
    fun dismissMenu() {
        menuState?.close()
    }
    init {
        scope.launch {
            controller.state.collect {
                feedState = it.feed
                interactionState = it.interactions
                if (portalSource == null) portalState = it.portal
            }
        }
        if (portalSource != null) {
            scope.launch { portalSource.collect { portalState = it.portal } }
        }
    }
    val type get() = feedState.type
    val filterMid get() = feedState.filterMid
    val items get() = feedState.items
    val loading get() = feedState.loading
    val refreshing get() = feedState.refreshing
    val error get() = feedState.error
    val hasMore get() = feedState.hasMore
    val authors get() = portalState.authors
    val liveUsers get() = portalState.liveUsers
    val liveCount get() = portalState.liveCount
    val authorsLoading get() = portalState.authorsLoading
    val authorsError get() = portalState.authorsError
    val authorsHasMore get() = portalState.authorsHasMore
    val activeLoading get() = loading
    val liked get() = interactionState.liked
    val likeCounts get() = interactionState.likeCounts

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
    /** 邻页预览：pager 的相邻页先显示这一份，滑过去时再换成常驻列表。 */
    private val previews = androidx.compose.runtime.mutableStateMapOf<Long, List<FeedItem>>()
    fun previewItems(mid: Long): List<FeedItem>? = previews[mid]
    /** 离开某个 UP 时把它当前的首屏留在预览缓存里，往回滑时不用重新等网络。 */
    fun seedPreview(mid: Long, items: List<FeedItem>) {
        if (mid > 0L && items.isNotEmpty()) previews[mid] = items
    }
    suspend fun prefetchUp(mid: Long, loggedIn: Boolean) {
        if (mid <= 0L || mid == filterMid || previews.containsKey(mid)) return
        val items = controller.prefetchUp(mid, loggedIn) ?: return
        previews[mid] = items
    }

    suspend fun toggleLike(item: FeedItem) = controller.toggleLike(item)
    suspend fun reportDynamic(item: FeedItem, reason: com.aritxonly.bdfun.core.domain.DynamicReportReason) =
        controller.reportDynamic(item, reason)
}
