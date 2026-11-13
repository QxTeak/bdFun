package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.feed.FeedItem

interface FeedDetailRepository {
    suspend fun detail(id: String): Outcome<FeedItem?>
    suspend fun userIpLocation(mid: Long): Outcome<String>
    suspend fun like(id: String, liked: Boolean): Outcome<Boolean>
}
