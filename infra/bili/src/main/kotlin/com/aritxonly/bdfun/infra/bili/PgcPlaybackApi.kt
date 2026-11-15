package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.pgc.BangumiEpisode
import com.aritxonly.bdfun.core.model.pgc.BangumiSeason
import com.aritxonly.bdfun.core.model.pgc.BangumiStream
import com.aritxonly.bdfun.infra.bili.mapper.mapBangumiSeason
import com.aritxonly.bdfun.infra.bili.mapper.mapBangumiStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val pgcPlayReferer = "https://www.bilibili.com/bangumi/play/"

/**
 * 番剧详情。信封是 `result` 不是 `data`，所以走 rawRequest 拿整包。
 */
internal suspend fun BiliApiContext.pgcSeason(seasonId: Long): BangumiSeason =
    withContext(Dispatchers.IO) {
        val root = rawRequest(
            "/pgc/view/web/season",
            mapOf("season_id" to seasonId.toString()),
            referer = pgcPlayReferer,
        )
        val code = root.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
        val result = root.optJSONObject("result")
            ?: throw BiliException(code, "番剧信息缺失")
        mapBangumiSeason(result)
    }

/**
 * 播放地址（PGC 专用）。`fnval=4048` 要 DASH；返回空就当作没拿到地址。
 */
internal suspend fun BiliApiContext.pgcStream(episode: BangumiEpisode): BangumiStream =
    withContext(Dispatchers.IO) {
        val root = rawRequest(
            "/pgc/player/web/playurl",
            mapOf(
                "ep_id" to episode.epId.toString(),
                "cid" to episode.cid.toString(),
                "avid" to episode.aid.toString(),
                "fnval" to "4048",
                "fourk" to "1",
            ),
            referer = pgcPlayReferer,
        )
        val code = root.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
        val result = root.optJSONObject("result")
            ?: throw BiliException(code, "播放地址缺失")
        mapBangumiStream(result) ?: throw BiliException(code, "没有可用的播放地址")
    }
