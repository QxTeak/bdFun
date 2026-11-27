package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.home.application.HomeFeedStateHolder
import com.aritxonly.bdfun.feature.home.application.HomeActionsStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 共享卡片在 app 模块，用 CompositionLocal 把“正在模糊”的 bvid 投影过去，避免加长 host 回调参数。 */
val LocalHomeBlurredBvids = androidx.compose.runtime.compositionLocalOf<Set<String>> { emptySet() }

/** 被“不感兴趣”的卡片只剩一个撤销按钮，参数是卡片自己的 bvid。 */
val LocalHomeUndoAction = androidx.compose.runtime.compositionLocalOf<(String) -> Unit> { {} }

/** bvid -> 模糊封面上的反馈提示；多张卡可以同时处于待撤销状态。 */
val LocalHomeUndoMessages = androidx.compose.runtime.compositionLocalOf<Map<String, String>> { emptyMap() }

/** 仅持有视图投影、下拉手势和列表滚动；业务状态由纯应用层控制器持有。 */
@Stable
class HomeFeedState internal constructor(private val controller: HomeFeedStateHolder, internal val actions: HomeActionsStateHolder, private val scope: CoroutineScope,
    private val optimizer: com.aritxonly.bdfun.core.application.RecommendationOptimizer? = null,
) {
    val gridState = LazyGridState()
    /** 省略号形变菜单：每张卡片自己的锚点 + 当前打开的视频。宿主挂在主壳采样树外。 */
    var menuState by mutableStateOf<com.aritxonly.teak.common.navigation.TeakMorphMenuState?>(null)
    var menuVideo by mutableStateOf<VideoItem?>(null)
    var menuViaLongPress by mutableStateOf(false)
    /**
     * 刚反馈“不感兴趣”的卡片：每张都留在列表里做静态模糊，互不影响；
     * key 是 bvid，value 是模糊封面上的提示（撤销失败时也用它显示错误）。
     */
    private val undoMessagesState = mutableStateMapOf<String, String>()
    val undoMessages: Map<String, String> get() = undoMessagesState
    val blurredBvids: Set<String> get() = undoMessagesState.keys
    private val undoInFlight = mutableSetOf<String>()
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

    /** 反馈成功：卡片留在原位并静态模糊；多张卡各自保留自己的提示，互不覆盖。 */
    fun markUndo(video: VideoItem, message: String = "将为您减少相关推荐") {
        undoMessagesState[video.bvid] = message
    }

    /** 先撤回该卡的服务端反馈，成功后只恢复这一张；失败保留提示供重试。 */
    fun undoDismiss(bvid: String) {
        if (bvid !in undoMessagesState || bvid in undoInFlight) return
        undoInFlight += bvid
        scope.launch {
            try {
                when (val result = controller.undoDismissed(bvid)) {
                    is com.aritxonly.bdfun.core.common.Outcome.Success -> undoMessagesState.remove(bvid)
                    is com.aritxonly.bdfun.core.common.Outcome.Failure -> {
                        undoMessagesState[bvid] = result.error.messageOr("撤销失败，请重试")
                    }
                }
            } finally {
                undoInFlight -= bvid
            }
        }
    }

    fun commitDismiss() {
        undoMessagesState.clear()
        controller.commitDismissed()
    }

    /** 底栏切页时关闭长按菜单，避免菜单留在新页面上并用旧锚点错位。 */
    fun dismissMenu() {
        menuState?.close()
    }
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
    blockUser: (suspend (Long) -> com.aritxonly.bdfun.core.common.Outcome<Unit>)? = null,
): HomeFeedState {
    val controller = HomeFeedStateHolder(feed, videos, optimizer, cache)
    val feedback = HomeActionsStateHolder(videos, actions, controller, blockUser)
    return HomeFeedState(controller, feedback, scope, optimizer)
}
