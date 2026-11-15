package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.pgc.BangumiRankBoard
import com.aritxonly.bdfun.core.model.pgc.BangumiRankTab

/**
 * 番剧页「排行榜」的读取接口。
 *
 * 榜单目录与榜单内容是两次请求：先 [tabs] 拿榜名/rank_id/oid，再按榜取 [board]。
 * 接口匿名可用，不需要登录态，也不做分页。
 */
interface PgcRankRepository {
    suspend fun tabs(indexId: Int): Outcome<List<BangumiRankTab>>

    suspend fun board(tab: BangumiRankTab): Outcome<BangumiRankBoard>

    /**
     * 需要大会员的 season_id 集合。
     *
     * 榜单接口（`/pgc/season/rank/v2/list`）不返回会员信息，只能拿网页版榜单的
     * `badge` 做对照；覆盖的是该 season_type 的热门榜前 99 个，够卡片角标用。
     */
    suspend fun vipSeasons(seasonType: Int): Outcome<Set<Long>>
}
