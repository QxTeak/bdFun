package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.RecommendationReason
import com.aritxonly.bdfun.core.model.video.VideoItem

interface RecommendationActionsRepository {
    suspend fun addWatchLater(aid: Long): Outcome<Boolean>
    suspend fun dislike(video: VideoItem, reason: RecommendationReason): Outcome<Unit>

    /** 撤销上一次“不感兴趣”反馈；App 推荐走 `/x/feed/dislike/cancel`。 */
    suspend fun cancelDislike(video: VideoItem, reason: RecommendationReason): Outcome<Unit>
}
