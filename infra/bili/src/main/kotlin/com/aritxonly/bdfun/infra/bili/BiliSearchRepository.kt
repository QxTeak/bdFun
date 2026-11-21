package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.SearchRepository
import com.aritxonly.bdfun.core.domain.SearchRequest
import kotlinx.coroutines.CancellationException

class BiliSearchRepository(private val backend: BiliBackend) : SearchRepository {
    override suspend fun hotKeywords() = searchOutcome { backend.hotKeywords() }
    override suspend fun suggestions(query: String) = searchOutcome { backend.searchSuggest(query) }
    override suspend fun users(keyword: String) = searchOutcome { backend.searchUsers(keyword) }
    override suspend fun videos(request: SearchRequest) = searchOutcome {
        backend.search(request.keyword, page = request.page, order = request.order, duration = request.duration,
            pubtimeBegin = request.pubtimeBegin, pubtimeEnd = request.pubtimeEnd)
    }
    override suspend fun pgc(keyword: String, bangumi: Boolean, page: Int) =
        searchOutcome { backend.searchPgc(keyword, bangumi, page) }
}

/** 搜索页原样展示异常 message；领域化后仍保留服务器原文和缺失时的原兜底。 */
internal suspend fun <T> searchOutcome(block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (cancel: CancellationException) {
    throw cancel
} catch (failure: Throwable) {
    Outcome.Failure(AppError.Unknown(failure.message ?: "搜索失败"))
}
