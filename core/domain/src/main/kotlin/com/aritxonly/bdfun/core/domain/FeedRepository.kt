package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.model.feed.FeedPage

interface FeedRepository {
    suspend fun recommend(page: Int): Outcome<List<VideoItem>>
    suspend fun dynamics(offset: String): Outcome<FeedPage>
}
