package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.PgcRankRepository
import com.aritxonly.bdfun.core.model.pgc.BangumiRankTab

/** 番剧页排行榜的仓储实现：只加错误映射，不复制请求逻辑。 */
class BiliPgcRankRepository(private val backend: BiliBackend) : PgcRankRepository {
    override suspend fun tabs(indexId: Int) = biliOutcome { backend.api.pgcRankTabs(indexId) }

    override suspend fun board(tab: BangumiRankTab) = biliOutcome { backend.api.pgcRankBoard(tab) }

    override suspend fun vipSeasons(seasonType: Int) = biliOutcome { backend.api.pgcRankVipSeasons(seasonType) }
}
