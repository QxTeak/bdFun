package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.LiveDiscoverRepository
import com.aritxonly.bdfun.core.domain.PgcDiscoverRepository
import com.aritxonly.bdfun.core.model.live.LiveCard
import com.aritxonly.bdfun.core.model.live.LiveHome
import com.aritxonly.bdfun.core.model.pgc.BangumiCard
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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

    override suspend fun timeline(): com.aritxonly.bdfun.core.common.Outcome<List<BangumiCard>> =
        biliOutcome {
            val cards = (api.pgcTimeline(1) + api.pgcTimeline(4)).distinctBy { it.seasonId }.take(6)
            // 头图要的是整部番剧的一句话简介，时间表本身只有集数文案，所以补一次 season 详情。
            coroutineScope {
                cards.map { card ->
                    async {
                        val season = try {
                            api.pgcSeason(card.seasonId)
                        } catch (cancel: kotlinx.coroutines.CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            null
                        }
                        if (season == null) card else card.copy(
                            // subtitle 有时是「已观看 x 万次」这类统计；简介优先取 evaluate 的首段。
                            subtitle = season.evaluate.replace('\n', ' ').trim().take(52)
                                .ifBlank { season.subtitle },
                        )
                    }
                }.awaitAll()
            }
        }
}
