package com.aritxonly.bdfun.feature.search.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.domain.SearchFilterState
import com.aritxonly.bdfun.core.domain.SearchRepository
import com.aritxonly.bdfun.core.domain.SearchRequest
import com.aritxonly.bdfun.core.model.pgc.SearchPgcItem
import com.aritxonly.bdfun.core.model.user.SearchUser
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class SearchPgcState(
    val bangumi: List<SearchPgcItem> = emptyList(),
    val films: List<SearchPgcItem> = emptyList(),
    val loading: Boolean = false,
    val loadedOnce: Boolean = false,
)

internal data class SearchUiState(
    val hotKeywords: List<com.aritxonly.bdfun.core.domain.SearchHotWord> = emptyList(),
    /** 搜索发现（底部板块）；与热榜是两份数据，互不阻塞。 */
    val discoveryKeywords: List<String> = emptyList(), val results: List<VideoItem> = emptyList(),
    val users: List<SearchUser> = emptyList(), val suggestions: List<String> = emptyList(),
    /** 番剧 / 影视 Tab 的结果；与视频/用户同时请求，互不阻塞。 */
    val pgc: SearchPgcState = SearchPgcState(),
    val loading: Boolean = false, val loadedOnce: Boolean = false, val error: String? = null,
    /** 往下翻页中的状态；与首屏 loading 分开，列表不会被清空。 */
    val loadingMore: Boolean = false,
    /** 已经没有下一页（或当前结果不足一页）。 */
    val end: Boolean = false,
    /** 用户 Tab 自己的一组分页状态：不能和视频结果共用一个 end，否则会互相提前结束。 */
    val userPaging: SearchUserPagingState = SearchUserPagingState(),
)

/** 用户 Tab 的分页状态；单独成组，避免主状态构造参数过长。 */
internal data class SearchUserPagingState(
    val loadingMore: Boolean = false,
    val end: Boolean = false,
)

internal class SearchStateHolder(
    private val repository: SearchRepository,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val mutable = MutableStateFlow(SearchUiState())
    val state = mutable.asStateFlow()
    private var searchRevision = 0
    private var suggestionRevision = 0
    private var lastUserKeyword: String? = null
    /** 用户 Tab 已加载到的页码；0 表示还没加载。 */
    private var usersPage = 0
    /** 当前结果对应的请求（含页码与筛选），翻下一页时在它上面 +1。 */
    private var lastRequest: SearchRequest? = null

    suspend fun loadHotKeywords(enabled: Boolean) {
        if (!enabled || mutable.value.hotKeywords.isNotEmpty()) return
        when (val result = repository.hotKeywords()) {
            is Outcome.Success -> mutable.update { it.copy(hotKeywords = result.value) }
            is Outcome.Failure -> Unit
        }
    }

    /** 搜索发现：首次进入拉一次；与 [loadHotKeywords] 独立，任一失败都不影响另一个板块。 */
    suspend fun loadDiscovery(enabled: Boolean) {
        if (!enabled || mutable.value.discoveryKeywords.isNotEmpty()) return
        when (val result = repository.discoveryKeywords()) {
            is Outcome.Success -> mutable.update { it.copy(discoveryKeywords = result.value) }
            is Outcome.Failure -> Unit
        }
    }

    suspend fun suggest(query: String, submitted: String?) {
        val revision = ++suggestionRevision
        val word = query.trim()
        if (submitted != null || word.isBlank()) {
            mutable.update { it.copy(suggestions = emptyList()) }
            return
        }
        wait(180)
        if (revision != suggestionRevision) return
        val result = repository.suggestions(word)
        if (revision != suggestionRevision) return
        mutable.update { it.copy(suggestions = when (result) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> emptyList()
        }) }
    }

    suspend fun search(submitted: String?, filters: SearchFilterState) {
        val revision = ++searchRevision
        if (submitted == null) {
            lastUserKeyword = null
            usersPage = 0
            lastRequest = null
            mutable.update { it.copy(results = emptyList(), users = emptyList(), pgc = SearchPgcState(),
                error = null, loadedOnce = false, loading = false, loadingMore = false, end = false,
                userPaging = SearchUserPagingState()) }
            return
        }
        lastRequest = null
        mutable.update { it.copy(loading = true, loadedOnce = false,
            pgc = it.pgc.copy(loading = true, loadedOnce = false),
            error = null, loadingMore = false, end = false,
            // 换关键词时别把上一组用户留在列表里冒充新结果。
            users = if (lastUserKeyword != submitted) emptyList() else it.users,
            userPaging = SearchUserPagingState()) }
        try {
            wait(if (lastUserKeyword != submitted) 90 else 180)
            if (revision != searchRevision) return
            if (lastUserKeyword != submitted) {
                // 换关键词时先丢弃旧分页游标，避免新关键词请求失败后还拿旧 page 去续页。
                usersPage = 0
                when (val result = repository.users(submitted, page = 1)) {
                    is Outcome.Success -> if (revision == searchRevision) {
                        lastUserKeyword = submitted
                        usersPage = 1
                        mutable.update { it.copy(
                            users = result.value,
                            userPaging = SearchUserPagingState(end = result.value.size < SEARCH_PAGE_SIZE),
                        ) }
                    }
                    is Outcome.Failure -> Unit
                }
            }
            if (revision != searchRevision) return
            val request = SearchRequest(
                submitted, filters.orderParam, filters.durationParam, filters.beginSecondsAt(nowMillis),
                if (filters.beginSecondsAt(nowMillis) != null) nowMillis() / 1000L else null,
                page = 1,
            )
            val result = repository.videos(request)
            if (revision != searchRevision) return
            mutable.update { current -> when (result) {
                is Outcome.Success -> {
                    lastRequest = request
                    current.copy(results = result.value, loadedOnce = true,
                        end = result.value.size < SEARCH_PAGE_SIZE)
                }
                is Outcome.Failure -> current.copy(error = when (val error = result.error) {
                    is AppError.Unknown -> error.message
                    else -> error.messageOr("搜索失败")
                }, loadedOnce = true)
            } }
            if (revision != searchRevision) return
            // 番剧/影视与视频结果并行请求，切 Tab 时不需要再等一次网络。
            val (bangumi, films) = coroutineScope {
                val bangumiAsync = async { repository.pgc(submitted, bangumi = true) }
                val filmsAsync = async { repository.pgc(submitted, bangumi = false) }
                bangumiAsync.await() to filmsAsync.await()
            }
            if (revision != searchRevision) return
            mutable.update {
                it.copy(
                    pgc = it.pgc.copy(
                        bangumi = if (bangumi is Outcome.Success) bangumi.value else emptyList(),
                        films = if (films is Outcome.Success) films.value else emptyList(),
                        loadedOnce = true,
                    ),
                )
            }
        } finally {
            if (revision == searchRevision) {
                mutable.update { it.copy(loading = false, pgc = it.pgc.copy(loading = false)) }
            }
        }
    }

    /** 列表滑到底：按当前关键词/筛选拉下一页，追加到现有结果后面。 */
    suspend fun loadMore() {
        val base = lastRequest ?: return
        val currentRevision = searchRevision
        if (mutable.value.loading || mutable.value.loadingMore || mutable.value.end) return
        val next = base.copy(page = base.page + 1)
        mutable.update { it.copy(loadingMore = true) }
        try {
            val result = repository.videos(next)
            if (currentRevision != searchRevision) return
            when (result) {
                is Outcome.Success -> {
                    lastRequest = next
                    mutable.update {
                        it.copy(
                            results = it.results + result.value,
                            loadingMore = false,
                            end = result.value.size < SEARCH_PAGE_SIZE,
                        )
                    }
                }
                is Outcome.Failure -> mutable.update { it.copy(loadingMore = false) }
            }
        } finally {
            if (currentRevision == searchRevision) mutable.update { it.copy(loadingMore = false) }
        }
    }

    /** 用户 Tab 滑到底：按同一关键词追加下一页用户。 */
    suspend fun loadMoreUsers() {
        val keyword = lastUserKeyword ?: return
        val snapshot = mutable.value
        if (snapshot.loading || snapshot.userPaging.loadingMore || snapshot.userPaging.end || usersPage <= 0) return
        val revision = searchRevision
        val next = usersPage + 1
        mutable.update { it.copy(userPaging = it.userPaging.copy(loadingMore = true)) }
        try {
            when (val result = repository.users(keyword, next)) {
                is Outcome.Success -> if (revision == searchRevision) {
                    usersPage = next
                    mutable.update { current ->
                        val known = current.users.mapTo(HashSet()) { it.mid }
                        current.copy(
                            users = current.users + result.value.filter { known.add(it.mid) },
                            userPaging = SearchUserPagingState(end = result.value.size < SEARCH_PAGE_SIZE),
                        )
                    }
                }
                is Outcome.Failure -> if (revision == searchRevision) {
                    mutable.update { it.copy(userPaging = it.userPaging.copy(loadingMore = false)) }
                }
            }
        } finally {
            if (revision == searchRevision) mutable.update { it.copy(userPaging = it.userPaging.copy(loadingMore = false)) }
        }
    }
}

/** 与 [com.aritxonly.bdfun.infra.bili] 搜索接口的 page_size 保持一致。 */
internal const val SEARCH_PAGE_SIZE = 20
