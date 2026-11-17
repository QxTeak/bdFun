package com.aritxonly.bdfun.feature.search.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.domain.SearchFilterState
import com.aritxonly.bdfun.core.domain.SearchRepository
import com.aritxonly.bdfun.core.domain.SearchRequest
import com.aritxonly.bdfun.core.model.user.SearchUser
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class SearchUiState(
    val hotKeywords: List<String> = emptyList(), val results: List<VideoItem> = emptyList(),
    val users: List<SearchUser> = emptyList(), val suggestions: List<String> = emptyList(),
    val loading: Boolean = false, val loadedOnce: Boolean = false, val error: String? = null,
    /** 往下翻页中的状态；与首屏 loading 分开，列表不会被清空。 */
    val loadingMore: Boolean = false,
    /** 已经没有下一页（或当前结果不足一页）。 */
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
    /** 当前结果对应的请求（含页码与筛选），翻下一页时在它上面 +1。 */
    private var lastRequest: SearchRequest? = null

    suspend fun loadHotKeywords(enabled: Boolean) {
        if (!enabled || mutable.value.hotKeywords.isNotEmpty()) return
        when (val result = repository.hotKeywords()) {
            is Outcome.Success -> mutable.update { it.copy(hotKeywords = result.value) }
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
            lastRequest = null
            mutable.update { it.copy(results = emptyList(), users = emptyList(), error = null,
                loadedOnce = false, loading = false, loadingMore = false, end = false) }
            return
        }
        lastRequest = null
        mutable.update { it.copy(loading = true, loadedOnce = false, error = null,
            loadingMore = false, end = false) }
        try {
            wait(if (lastUserKeyword != submitted) 90 else 180)
            if (revision != searchRevision) return
            if (lastUserKeyword != submitted) {
                when (val result = repository.users(submitted)) {
                    is Outcome.Success -> if (revision == searchRevision) {
                        lastUserKeyword = submitted
                        mutable.update { it.copy(users = result.value) }
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
        } finally {
            if (revision == searchRevision) mutable.update { it.copy(loading = false) }
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
}

/** 与 [com.aritxonly.bdfun.infra.bili] 搜索接口的 page_size 保持一致。 */
internal const val SEARCH_PAGE_SIZE = 20
