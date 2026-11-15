package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.LiveDiscoverRepository
import com.aritxonly.bdfun.core.domain.PgcDiscoverRepository
import com.aritxonly.bdfun.core.model.live.LiveCard
import com.aritxonly.bdfun.core.model.live.LiveHome
import com.aritxonly.bdfun.core.model.pgc.BangumiCard

/** 首页直播段：包装 infra:bili 里的 PiliPlus 同款接口。 */
class BiliLiveDiscoverRepository(private val api: BiliBackend) : LiveDiscoverRepository {
    override suspend fun home(page: Int): com.aritxonly.bdfun.core.common.Outcome<LiveHome> =
        biliOutcome { api.liveHome(page) }

    override suspend fun list(page: Int, parentAreaId: Long, areaId: Long): com.aritxonly.bdfun.core.common.Outcome<List<LiveCard>> =
        biliOutcome { api.liveRecommend(page, parentAreaId, areaId) }

    override suspend fun followers(): com.aritxonly.bdfun.core.common.Outcome<List<com.aritxonly.bdfun.core.model.live.LiveFollower>> =
        biliOutcome { api.liveFollowers() }
}

/** 首页番剧段：最近追番 + 推荐。 */
class BiliPgcDiscoverRepository(private val api: BiliBackend) : PgcDiscoverRepository {
    override suspend fun following(page: Int): com.aritxonly.bdfun.core.common.Outcome<List<BangumiCard>> =
        biliOutcome { api.pgcFollowing(page) }

    override suspend fun recommend(page: Int): com.aritxonly.bdfun.core.common.Outcome<List<BangumiCard>> =
        biliOutcome { api.pgcIndex(page) }
}
