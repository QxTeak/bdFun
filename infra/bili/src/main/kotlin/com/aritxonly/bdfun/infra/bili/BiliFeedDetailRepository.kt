package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.FeedDetailRepository
import com.aritxonly.bdfun.core.model.feed.FeedItem

class BiliFeedDetailRepository(
    private val loadDetail: suspend (String) -> FeedItem?,
    private val loadIp: suspend (Long) -> String,
    private val sendLike: suspend (String, Boolean) -> Boolean,
) : FeedDetailRepository {
    override suspend fun detail(id: String) = biliOutcome { loadDetail(id) }
    override suspend fun userIpLocation(mid: Long) = biliOutcome { loadIp(mid) }
    override suspend fun like(id: String, liked: Boolean) = biliOutcome { sendLike(id, liked) }
}
