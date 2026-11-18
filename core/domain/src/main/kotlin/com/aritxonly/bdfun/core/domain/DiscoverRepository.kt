package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.live.LiveCard
import com.aritxonly.bdfun.core.model.live.LiveFollower
import com.aritxonly.bdfun.core.model.live.LiveHome
import com.aritxonly.bdfun.core.model.pgc.BangumiCard

/**
 * 首页「直播」段的读取接口。实现放在 infra:bili，界面只依赖这层。
 */
interface LiveDiscoverRepository {
    suspend fun home(page: Int = 1): Outcome<LiveHome>

    suspend fun list(page: Int = 1, parentAreaId: Long = 0L, areaId: Long = 0L): Outcome<List<LiveCard>>

    /** 我的关注里正在直播的 UP（动态 portal 的 live_users，和动态页头像行同源）。 */
    suspend fun followers(): Outcome<List<LiveFollower>>
}

/**
 * 首页「番剧」段的读取接口：最近追番 + 推荐。
 */
interface PgcDiscoverRepository {
    suspend fun following(page: Int = 1): Outcome<List<BangumiCard>>

    suspend fun recommend(page: Int = 1): Outcome<List<BangumiCard>>

    /** 正在更新的新番（番剧 + 国创时间表），给首页头图用；不带简介，保证头图先出。 */
    suspend fun timeline(): Outcome<List<BangumiCard>>

    /** 单部番剧的一句话简介；头图先出标题/封面，再由它补齐副标题。 */
    suspend fun seasonIntro(seasonId: Long): Outcome<String>
}

/**
 * 首页「影视」段的读取接口：与番剧页同构，但数据覆盖电影/电视剧/纪录片/综艺。
 *
 * 实现放在 infra:bili；聚合策略（推荐按 season_type 交错、时间表合并）
 * 由实现决定，页面状态与番剧页共用同一套。
 */
interface CinemaDiscoverRepository : PgcDiscoverRepository
