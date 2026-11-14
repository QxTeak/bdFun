package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.PgcPlaybackRepository
import com.aritxonly.bdfun.core.model.pgc.BangumiEpisode

/** 番剧播放仓储：只做错误映射，请求与解析在 PgcPlaybackApi / mapper。 */
class BiliPgcPlaybackRepository(private val backend: BiliBackend) : PgcPlaybackRepository {
    override suspend fun season(seasonId: Long) = biliOutcome { backend.api.pgcSeason(seasonId) }

    override suspend fun stream(episode: BangumiEpisode) = biliOutcome { backend.api.pgcStream(episode) }
}
