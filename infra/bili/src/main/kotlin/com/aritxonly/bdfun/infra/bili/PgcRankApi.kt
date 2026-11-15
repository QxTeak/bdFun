package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.pgc.BangumiRankBoard
import com.aritxonly.bdfun.core.model.pgc.BangumiRankTab
import com.aritxonly.bdfun.infra.bili.mapper.mapRankBoard
import com.aritxonly.bdfun.infra.bili.mapper.mapRankTabs
import com.aritxonly.bdfun.infra.bili.mapper.mapRankVipSeasons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 番剧页排行榜：番剧(1) / 国创(4) 两个 index。电影等其它 index 本页不接。 */
internal const val PGC_RANK_INDEX_BANGUMI = 1
internal const val PGC_RANK_INDEX_GUOCHUANG = 4

private const val pgcRankReferer = "https://www.bilibili.com/anime/"

/**
 * 榜单目录。`data` 是数组，走 rawRequest 拿整包再自己判 code——
 * dataRequest 只会返回 `data` 对象，数组会被吞掉。
 */
internal suspend fun BiliApiContext.pgcRankTabs(indexId: Int): List<BangumiRankTab> =
    withContext(Dispatchers.IO) {
        val root = rawRequest(
            "/pgc/season/rank/tabs",
            mapOf("index_id" to indexId.toString()),
            referer = pgcRankReferer,
        )
        val code = root.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
        mapRankTabs(root.optJSONArray("data"))
    }

/** 单个榜单内容；一次返回整榜，不分页。 */
internal suspend fun BiliApiContext.pgcRankBoard(tab: BangumiRankTab): BangumiRankBoard =
    withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/pgc/season/rank/v2/list",
            mapOf("oid" to tab.oid, "rank_id" to tab.rankId.toString()),
            referer = pgcRankReferer,
        )
        mapRankBoard(data)
    }

/**
 * 需要大会员的 season_id 集合。
 *
 * `/pgc/season/rank/v2/list` 不带会员字段，这里用网页版榜单（有 `badge`/`badge_info`）做对照。
 * 拉取失败就返回空集合：角标少显示，不影响榜单本身。
 */
internal suspend fun BiliApiContext.pgcRankVipSeasons(seasonType: Int): Set<Long> =
    withContext(Dispatchers.IO) {
        // 这个接口的信封是 result 不是 data，所以走 rawRequest 自己取。
        val root = rawRequest(
            "/pgc/web/rank/list",
            mapOf("season_type" to seasonType.toString(), "day" to "3"),
            referer = pgcRankReferer,
        )
        val code = root.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
        val payload = root.optJSONObject("result") ?: root.optJSONObject("data") ?: return@withContext emptySet()
        mapRankVipSeasons(payload)
    }
