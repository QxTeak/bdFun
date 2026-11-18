package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.CinemaDiscoverRepository
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
            // 两个时间表并行；这里只返回标题/封面，简介由 seasonIntro 后补，保证头图先出。
            coroutineScope {
                val bangumi = async { api.pgcTimeline(1) }
                val guochuang = async { api.pgcTimeline(4) }
                (bangumi.await() + guochuang.await()).distinctBy { it.seasonId }
            }
        }

    override suspend fun seasonIntro(seasonId: Long): com.aritxonly.bdfun.core.common.Outcome<String> =
        biliOutcome { api.pgcSeasonIntroText(seasonId) }
}

/** 影视追剧列表的 type（番剧是 1）。 */
private const val CINEMA_FOLLOW_TYPE = 2
/** 影视聚合的类型：电影、电视剧、纪录片、综艺。 */
private val CINEMA_SEASON_TYPES = listOf(2, 5, 3, 7)

/**
 * 首页「影视」段：电影/电视剧/纪录片/综艺的聚合读取。
 *
 * 推荐按类型并行拉同一页，再 round-robin 交错，保证第一屏四种类型都有；
 * 时间表同样并行合并。任一类型失败只是少一段内容，不影响其余类型。
 */
class BiliCinemaDiscoverRepository(private val api: BiliBackend) : CinemaDiscoverRepository {
    override suspend fun following(page: Int): com.aritxonly.bdfun.core.common.Outcome<List<BangumiCard>> =
        biliOutcome { api.pgcFollowing(page, type = CINEMA_FOLLOW_TYPE) }

    override suspend fun recommend(page: Int): com.aritxonly.bdfun.core.common.Outcome<List<BangumiCard>> =
        biliOutcome {
            val pages = coroutineScope {
                CINEMA_SEASON_TYPES.map { type ->
                    async { api.pgcIndex(page, seasonType = type) }
                }.awaitAll()
            }
            interleaveCinemaCards(pages)
        }

    override suspend fun timeline(): com.aritxonly.bdfun.core.common.Outcome<List<BangumiCard>> =
        biliOutcome {
            coroutineScope {
                CINEMA_SEASON_TYPES.map { type -> async { api.pgcTimeline(type) } }.awaitAll()
            }.flatten().distinctBy { it.seasonId }
        }

    override suspend fun seasonIntro(seasonId: Long): com.aritxonly.bdfun.core.common.Outcome<String> =
        biliOutcome { api.pgcSeasonIntroText(seasonId) }
}

/** 每页按类型轮流取一张，再按 seasonId 去重；顺序稳定，分页追加不会跳位。 */
internal fun interleaveCinemaCards(pages: List<List<BangumiCard>>): List<BangumiCard> {
    if (pages.isEmpty()) return emptyList()
    val result = ArrayList<BangumiCard>(pages.sumOf { it.size })
    val seen = HashSet<Long>()
    val longest = pages.maxOf { it.size }
    for (index in 0 until longest) {
        for (page in pages) {
            val card = page.getOrNull(index) ?: continue
            if (seen.add(card.seasonId)) result += card
        }
    }
    return result
}

/** 详情页一句话简介：换行压平并截断；接口没有 evaluate 时退回 subtitle。 */
private suspend fun BiliBackend.pgcSeasonIntroText(seasonId: Long): String {
    val season = pgcSeason(seasonId)
    return season.evaluate.replace('\n', ' ').trim().take(52).ifBlank { season.subtitle }
}
