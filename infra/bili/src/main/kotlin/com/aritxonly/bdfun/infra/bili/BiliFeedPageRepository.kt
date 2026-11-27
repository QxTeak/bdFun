package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.DynamicReportReason
import com.aritxonly.bdfun.core.domain.FeedPageRepository
import com.aritxonly.bdfun.core.model.feed.*

class BiliFeedPageRepository(
    private val page: suspend (String, String, Long?) -> FeedPage,
    private val portal: suspend () -> FeedPortal,
    private val upList: suspend (String) -> FeedUpPage,
    private val like: suspend (String, Boolean) -> Boolean,
    private val report: suspend (String, Long, DynamicReportReason) -> Unit,
) : FeedPageRepository {
    override suspend fun dynamicPage(type: String, offset: String, hostMid: Long?) = biliOutcome { page(type, offset, hostMid) }
    override suspend fun dynamicPortal() = biliOutcome { portal() }
    override suspend fun dynamicUpList(offset: String) = biliOutcome { upList(offset) }
    override suspend fun likeFeed(id: String, like: Boolean) = biliOutcome { like(id, like) }
    override suspend fun reportDynamic(dynamicId: String, accusedMid: Long, reason: DynamicReportReason) =
        biliOutcome { report(dynamicId, accusedMid, reason) }
}
