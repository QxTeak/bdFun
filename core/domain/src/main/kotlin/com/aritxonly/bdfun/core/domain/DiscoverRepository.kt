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
}
