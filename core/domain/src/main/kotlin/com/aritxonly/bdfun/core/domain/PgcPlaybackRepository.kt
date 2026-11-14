package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.pgc.BangumiEpisode
import com.aritxonly.bdfun.core.model.pgc.BangumiSeason
import com.aritxonly.bdfun.core.model.pgc.BangumiStream

/**
 * 番剧播放所需的只读接口：剧集详情 + 播放地址。
 *
 * 播放地址走 PGC 专用接口（`/pgc/player/web/playurl`），普通视频的
 * `/x/player/playurl` 对番剧只能拿到低清，所以不复用视频那条链路。
 */
interface PgcPlaybackRepository {
    suspend fun season(seasonId: Long): Outcome<BangumiSeason>

    suspend fun stream(episode: BangumiEpisode): Outcome<BangumiStream>
}
