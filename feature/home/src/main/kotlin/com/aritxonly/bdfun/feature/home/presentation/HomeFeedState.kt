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
    /** 省略号形变菜单：每张卡片自己的锚点 + 当前打开的视频。宿主挂在主壳采样树外。 */
    var menuState by mutableStateOf<com.aritxonly.teak.common.navigation.TeakMorphMenuState?>(null)
    var menuVideo by mutableStateOf<VideoItem?>(null)
    var menuViaLongPress by mutableStateOf(false)
    val optimizationEnabled get() = optimizer?.state?.value?.enabled == true
    internal fun exposureTicket() = controller.exposureTicket()
    internal fun expose(video: VideoItem, ticket: com.aritxonly.bdfun.core.application.RecommendationTicket) = controller.expose(video, ticket)
    internal fun open(video: VideoItem) = controller.open(video)
    /**
     * 逐字段投影：以前所有 getter 共用一个 `mutableStateOf` 快照，翻页时
     * `loadingMore` 一翻转就会让读 `videos` 的网格、读 `refreshing` 的顶栏一起失效。
     * 拆开后各读各的，只有真正变了的字段才触发重组。
     */
    private var videosState by mutableStateOf(controller.state.value.videos)
    private var loadingState by mutableStateOf(controller.state.value.loading)
    private var refreshingState by mutableStateOf(controller.state.value.refreshing)
    private var loadingMoreState by mutableStateOf(controller.state.value.loadingMore)
    private var endReachedState by mutableStateOf(controller.state.value.endReached)
    private var refreshRequestState by mutableStateOf(controller.state.value.refreshRequest)
    private var errorState by mutableStateOf(controller.state.value.error)
    private var loadMoreErrorState by mutableStateOf(controller.state.value.loadMoreError)
    private var filteredPageState by mutableStateOf(controller.state.value.filteredPage)
    private var previousState = controller.state.value
    var pullFraction by mutableFloatStateOf(0f)
    var pullOffsetDp by mutableFloatStateOf(0f)
    val videos get() = videosState
    val loading get() = loadingState
    val refreshing get() = refreshingState
    val loadingMore get() = loadingMoreState
    val endReached get() = endReachedState
    val refreshRequest get() = refreshRequestState
    val error get() = errorState?.messageOr("网络请求失败")
        ?: if (videosState.isEmpty() && !loadingState) "推荐流为空" else null
    val loadMoreError get() = loadMoreErrorState?.messageOr("加载失败")
        ?: if (filteredPageState) "本页内容已过滤，点击继续加载" else null
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
                val scroll = next.scrollRevision != previousState.scrollRevision
                previousState = next
                videosState = next.videos
                loadingState = next.loading
                refreshingState = next.refreshing
                loadingMoreState = next.loadingMore
                endReachedState = next.endReached
                refreshRequestState = next.refreshRequest
                errorState = next.error
                loadMoreErrorState = next.loadMoreError
                filteredPageState = next.filteredPage
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
    cache: com.aritxonly.bdfun.core.domain.HomeFeedCache? = null,
): HomeFeedState {
    val controller = HomeFeedStateHolder(feed, videos, optimizer, cache)
    val feedback = HomeActionsStateHolder(videos, actions, controller)
    return HomeFeedState(controller, feedback, scope, optimizer)
}
