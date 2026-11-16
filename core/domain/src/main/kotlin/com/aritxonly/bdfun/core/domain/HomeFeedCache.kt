package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.VideoItem

/** 缓存只用于首屏兜底，更新失败不能清空已有列表。 */
interface HomeFeedCache {
    suspend fun read(): Outcome<List<VideoItem>>
    suspend fun write(items: List<VideoItem>): Outcome<Unit>
}
