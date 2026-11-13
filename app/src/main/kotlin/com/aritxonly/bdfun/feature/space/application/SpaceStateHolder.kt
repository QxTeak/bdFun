package com.aritxonly.bdfun.feature.space.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.SpaceRepository
import com.aritxonly.bdfun.core.model.user.SpaceSeason
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 资料与分页只依赖仓储；头图解码、布局、手势留在表现层。 */
internal class SpaceStateHolder(
    private val mid: Long, private val repository: SpaceRepository,
    private val currentOwner: () -> Boolean = { true },
) {
    private val mutable = MutableStateFlow(SpaceUiState())
    val state = mutable.asStateFlow()
    private var bootstrapping = false
    private var closed = false
    private var followBaseline: Boolean? = null
    private val active get() = !closed && currentOwner()

    fun close() {
        if (closed) return
        closed = true
        mutable.update { it.copy(
            profile = it.profile.copy(loading = false, settled = true, followBusy = false,
                followed = followBaseline ?: it.profile.followed),
            videos = it.videos.copy(loading = false), articles = it.articles.copy(loading = false),
            bangumi = it.bangumi.copy(loading = false), shop = it.shop.copy(loading = false),
            collection = it.collection.copy(loading = false, revision = it.collection.revision + 1),
            dynamics = it.dynamics.copy(loading = false),
        ) }
        followBaseline = null
    }

    private suspend fun checkActive() {
        currentCoroutineContext().ensureActive()
        if (!active) throw CancellationException("个人主页已失效")
    }
    private suspend fun <T> requestCurrent(action: suspend () -> T): T {
        checkActive()
        val result = action()
        checkActive()
        return result
    }
    private suspend fun <T> Deferred<T>.awaitCurrent(): T {
        val result = await()
        checkActive()
        return result
    }

    /** 滚动事件发生时读最新页码，不能固定在 LaunchedEffect 初次组合的投影上。 */
    fun nextPage(tab: Int, submissionMode: Int): SpacePageRequest? {
        if (!active) return null
        val current = mutable.value
        return when {
            tab == 1 && !current.dynamics.loading && !current.dynamics.end -> SpacePageRequest.Feeds
            tab == 2 && submissionMode == 0 && !current.videos.loading && !current.videos.end ->
                SpacePageRequest.Videos(current.videos.page + 1)
            tab == 2 && submissionMode == 1 && !current.articles.loading && !current.articles.end ->
                SpacePageRequest.Articles(current.articles.page + 1)
            tab == 3 && !current.bangumi.loading && !current.bangumi.end ->
                SpacePageRequest.Bangumi(current.bangumi.page + 1)
            else -> null
        }
    }

    suspend fun load(isSelf: Boolean, loggedIn: Boolean) {
        if (!active || bootstrapping) return
        bootstrapping = true
        mutable.update { it.copy(profile = it.profile.copy(loading = it.profile.space == null, error = null)) }
        try {
            coroutineScope {
                val profile = async { requestCurrent { repository.profile(mid) } }
                val seasons = async { requestCurrent { repository.seasons(mid) } }
                val dynamics = async { requestCurrent { repository.dynamics(mid, "") } }
                val relation = async { if (!isSelf && loggedIn) requestCurrent { repository.relation(mid) } else null }
                val videos = async { requestCurrent { repository.videos(mid, 1) } }
                val fetched = profile.awaitCurrent()
                mutable.update { current -> current.copy(profile = when (fetched) {
                    is Outcome.Success -> current.profile.copy(space = fetched.value, settled = true)
                    is Outcome.Failure -> current.profile.copy(settled = true, error = fetched.error.messageOr("个人主页加载失败"))
                }) }
                val seasonResult = seasons.awaitCurrent()
                if (seasonResult is Outcome.Success) mutable.update { it.copy(seasons = seasonResult.value) }
                val dynamicResult = dynamics.awaitCurrent()
                // main首轮失败保留原游标/可继续状态，进入动态Tab仍可重试。
                if (dynamicResult is Outcome.Success) applyFeeds(dynamicResult, append = false)
                val attribute = relation.awaitCurrent()
                mutable.update { it.copy(profile = it.profile.copy(followed = when {
                    isSelf -> false
                    attribute is Outcome.Success -> attribute.value == 2 || attribute.value == 6
                    fetched is Outcome.Success -> fetched.value.isFollowed
                    else -> it.profile.followed
                })) }
                val fallback = videos.awaitCurrent()
                val included = (fetched as? Outcome.Success)?.value?.videos.orEmpty()
                val first = if (included.isNotEmpty()) included else (fallback as? Outcome.Success)?.value.orEmpty()
                mutable.update { it.copy(videos = it.videos.copy(items = first, page = 1, loaded = true,
                    end = if (included.isNotEmpty()) first.size < 30 else first.isEmpty(),
                    error = (fallback as? Outcome.Failure)?.error?.messageOr("投稿加载失败"))) }
            }
        } finally {
            bootstrapping = false
            if (!closed) mutable.update { it.copy(profile = it.profile.copy(loading = false, settled = true)) }
        }
    }

    suspend fun loadVideos(page: Int) = loadContent(page, { it.videos }, { state, content -> state.copy(videos = content) },
        { repository.videos(mid, page) }, { it.bvid })
    suspend fun loadArticles(page: Int) = loadContent(page, { it.articles }, { state, content -> state.copy(articles = content) },
        { repository.articles(mid, page) }, { it.id })
    suspend fun loadBangumi(page: Int) = loadContent(page, { it.bangumi }, { state, content -> state.copy(bangumi = content) },
        { repository.bangumi(mid, page) }, { it.seasonId })
    suspend fun loadShop() {
        if (!active || mutable.value.shop.loaded) return
        loadContent(1, { it.shop }, { state, content -> state.copy(shop = content) }, { repository.shop(mid) }, { it })
    }

    suspend fun loadCollection(collection: SpaceSeason) {
        if (!active) return
        // 用户可连续切合集：每次选择都使前一次请求过期，避免标题与列表错配。
        val revision = mutable.value.collection.revision + 1
        mutable.update { it.copy(collection = it.collection.copy(revision = revision, loading = true, error = null)) }
        try {
            val result = requestCurrent { repository.collection(mid, collection) }
            mutable.update { if (it.collection.revision != revision) it else it.copy(collection = when (result) {
                is Outcome.Success -> it.collection.copy(items = result.value, loaded = true)
                is Outcome.Failure -> it.collection.copy(error = result.error.messageOr("合集加载失败"))
            }) }
        } finally { mutable.update { if (it.collection.revision != revision) it else it.copy(collection = it.collection.copy(loading = false)) } }
    }

    private suspend fun <T, K> loadContent(
        page: Int, get: (SpaceUiState) -> SpaceContentState<T>,
        set: (SpaceUiState, SpaceContentState<T>) -> SpaceUiState,
        request: suspend () -> Outcome<List<T>>, key: (T) -> K,
    ) {
        val initial = get(mutable.value)
        if (!active || initial.loading) return
        mutable.update { set(it, get(it).copy(loading = true, error = null)) }
        try {
            val result = requestCurrent { request() }
            mutable.update { current -> set(current, when (result) {
                is Outcome.Success -> get(current).copy(items = if (page <= 1) result.value
                    else (get(current).items + result.value).distinctBy(key), page = page,
                    end = result.value.isEmpty(), loaded = true)
                is Outcome.Failure -> get(current).copy(error = result.error.messageOr("加载失败"), end = true)
            }) }
        } finally { if (!closed) mutable.update { set(it, get(it).copy(loading = false)) } }
    }

    suspend fun loadMoreFeeds() {
        val initial = mutable.value.dynamics
        if (!active || initial.loading || initial.end) return
        mutable.update { it.copy(dynamics = it.dynamics.copy(loading = true, error = null)) }
        try { applyFeeds(requestCurrent { repository.dynamics(mid, initial.offset) }, initial.offset.isNotBlank()) }
        finally { if (!closed) mutable.update { it.copy(dynamics = it.dynamics.copy(loading = false)) } }
    }

    private fun applyFeeds(result: Outcome<com.aritxonly.bdfun.core.model.feed.FeedPage>, append: Boolean) {
        mutable.update { it.copy(dynamics = when (result) {
            is Outcome.Success -> it.dynamics.copy(items = if (append) (it.dynamics.items + result.value.items).distinctBy { item -> item.id }
                else result.value.items, offset = result.value.offset, end = !result.value.hasMore || result.value.offset.isBlank())
            is Outcome.Failure -> it.dynamics.copy(end = true, error = result.error.messageOr("动态加载失败"))
        }) }
    }

    suspend fun toggleFollow(): String? {
        val initial = mutable.value.profile
        if (!active || initial.followBusy) return null
        followBaseline = initial.followed
        val target = !initial.followed
        mutable.update { it.copy(profile = it.profile.copy(followed = target, followBusy = true)) }
        var committed = false
        try {
            return when (val result = requestCurrent { repository.follow(mid, target) }) {
                is Outcome.Success -> { committed = result.value; if (committed) {
                    if (target) "关注成功" else "已取消关注"
                } else "操作失败" }
                is Outcome.Failure -> result.error.messageOr("操作失败")
            }
        } finally {
            if (!closed) mutable.update { it.copy(profile = it.profile.copy(followBusy = false,
                followed = if (committed) target else initial.followed)) }
            followBaseline = null
        }
    }
}
