package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.home.application.HomeFeedStateHolder
import com.aritxonly.bdfun.feature.home.application.HomeActionsStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 仅持有视图投影、下拉手势和列表滚动；业务状态由纯应用层控制器持有。 */
@Stable
class HomeFeedState internal constructor(private val controller: HomeFeedStateHolder, internal val actions: HomeActionsStateHolder, scope: CoroutineScope,
    private val optimizer: com.aritxonly.bdfun.core.application.RecommendationOptimizer? = null,
) {
    val gridState = LazyGridState()
    val optimizationEnabled get() = optimizer?.state?.value?.enabled == true
    internal fun exposureTicket() = controller.exposureTicket()
    internal fun expose(video: VideoItem, ticket: com.aritxonly.bdfun.core.application.RecommendationTicket) = controller.expose(video, ticket)
    internal fun open(video: VideoItem) = controller.open(video)
    private var observed by mutableStateOf(controller.state.value)
    var pullFraction by mutableFloatStateOf(0f)
    var pullOffsetDp by mutableFloatStateOf(0f)
    val videos get() = observed.videos
    val loading get() = observed.loading
    val refreshing get() = observed.refreshing
    val loadingMore get() = observed.loadingMore
    val endReached get() = observed.endReached
    val refreshRequest get() = observed.refreshRequest
    val error get() = observed.error?.messageOr("网络请求失败")
        ?: if (observed.videos.isEmpty() && !observed.loading) "推荐流为空" else null
    val loadMoreError get() = observed.loadMoreError?.messageOr("加载失败")
        ?: if (observed.filteredPage) "本页内容已过滤，点击继续加载" else null
    init {
        optimizer?.let { optimization -> scope.launch {
            var previous = optimization.state.value.generation
            optimization.state.collect { mode ->
                if (mode.generation != previous) {
                    previous = mode.generation
                    controller.modeChanged()
                }
            }
        } }
        scope.launch {
            controller.state.collect { next ->
                val scroll = next.scrollRevision != observed.scrollRevision
                observed = next
                if (scroll) gridState.scrollToItem(0)
            }
        }
    }
    fun close() { controller.close(); actions.close() }
    fun requestRefresh() = controller.requestRefresh()
    fun dismissRecommendation(video: VideoItem, creator: Boolean = false, category: String? = null) =
        controller.dismissRecommendation(video, creator, category)
    suspend fun ensureLoaded() = controller.ensureLoaded()
    suspend fun refresh() = controller.refresh()
    suspend fun loadMore() = controller.loadMore()
}

/** app提供仓储与原页面作用域；模块内部组装状态，避免壳访问internal实现。 */
fun createHomeFeedState(
    feed: com.aritxonly.bdfun.core.domain.FeedRepository,
    videos: com.aritxonly.bdfun.core.domain.VideoRepository,
    actions: com.aritxonly.bdfun.core.domain.RecommendationActionsRepository,
    scope: CoroutineScope,
    optimizer: com.aritxonly.bdfun.core.application.RecommendationOptimizer? = null,
): HomeFeedState {
    val controller = HomeFeedStateHolder(feed, videos, optimizer)
    val feedback = HomeActionsStateHolder(videos, actions, controller)
    return HomeFeedState(controller, feedback, scope, optimizer)
}
