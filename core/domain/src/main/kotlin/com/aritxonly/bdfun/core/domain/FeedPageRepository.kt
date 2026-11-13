package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.feed.*

interface FeedPageRepository {
    suspend fun dynamicPage(type: String, offset: String, hostMid: Long?): Outcome<FeedPage>
    suspend fun dynamicPortal(): Outcome<FeedPortal>
    suspend fun dynamicUpList(offset: String): Outcome<FeedUpPage>
    suspend fun likeFeed(id: String, like: Boolean): Outcome<Boolean>
}
