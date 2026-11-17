package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.user.SearchUser
import com.aritxonly.bdfun.core.model.video.VideoItem

data class SearchRequest(
    val keyword: String, val order: String, val duration: Int,
    val pubtimeBegin: Long?, val pubtimeEnd: Long?,
    /** 从 1 开始的页码；搜索页往下翻时逐页递增。 */
    val page: Int = 1,
)

interface SearchRepository {
    suspend fun hotKeywords(): Outcome<List<String>>
    suspend fun suggestions(query: String): Outcome<List<String>>
    suspend fun users(keyword: String): Outcome<List<SearchUser>>
    suspend fun videos(request: SearchRequest): Outcome<List<VideoItem>>
}
