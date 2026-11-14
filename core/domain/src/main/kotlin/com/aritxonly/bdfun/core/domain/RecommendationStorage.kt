package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.feed.RecommendationArchive

/** 实现负责原子写入和格式校验；仅由应用层在 IO 调度器调用。 */
interface RecommendationStorage {
    fun read(): RecommendationArchive
    fun write(archive: RecommendationArchive)
}
