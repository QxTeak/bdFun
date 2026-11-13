package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.FeedPageRepository
import com.aritxonly.bdfun.core.model.feed.*

class BiliFeedPageRepository(
    private val page: suspend (String, String, Long?) -> FeedPage,
    private val portal: suspend () -> FeedPortal,
    private val upList: suspend (String) -> FeedUpPage,
    private val like: suspend (String, Boolean) -> Boolean,
) : FeedPageRepository {
    override suspend fun dynamicPage(type: String, offset: String, hostMid: Long?) = biliOutcome { page(type, offset, hostMid) }
    override suspend fun dynamicPortal() = biliOutcome { portal() }
    override suspend fun dynamicUpList(offset: String) = biliOutcome { upList(offset) }
    override suspend fun likeFeed(id: String, like: Boolean) = biliOutcome { like(id, like) }
}
