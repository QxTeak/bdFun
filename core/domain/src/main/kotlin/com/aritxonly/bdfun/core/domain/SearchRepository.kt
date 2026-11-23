package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.pgc.SearchPgcItem
import com.aritxonly.bdfun.core.model.user.SearchUser
import com.aritxonly.bdfun.core.model.video.VideoItem

data class SearchRequest(
    val keyword: String, val order: String, val duration: Int,
    val pubtimeBegin: Long?, val pubtimeEnd: Long?,
    /** 从 1 开始的页码；搜索页往下翻时逐页递增。 */
    val page: Int = 1,
)

/**
 * 热搜词条。
 *
 * [showName] 是 B 站给的展示文案（偶尔和搜索词不同）；[icon] 是词条后面那枚小标，
 * 例如「独家」「热」，B 站直接下发一张透明 PNG，空串表示没有。
 */
data class SearchHotWord(
    val keyword: String,
    val showName: String = keyword,
    val icon: String = "",
)

interface SearchRepository {
    suspend fun hotKeywords(): Outcome<List<SearchHotWord>>
    /** 搜索发现：与热榜是两份数据，按 PiliPlus 的 `/x/v2/search/recommend` 取。 */
    suspend fun discoveryKeywords(): Outcome<List<String>>
    suspend fun suggestions(query: String): Outcome<List<String>>
    suspend fun users(keyword: String, page: Int = 1): Outcome<List<SearchUser>>
    suspend fun videos(request: SearchRequest): Outcome<List<VideoItem>>
    /** 番剧/影视搜索；[bangumi] 为 true 用 media_bangumi，否则 media_ft。 */
    suspend fun pgc(keyword: String, bangumi: Boolean, page: Int = 1): Outcome<List<SearchPgcItem>>
}
