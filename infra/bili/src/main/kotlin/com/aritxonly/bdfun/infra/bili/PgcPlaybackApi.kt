package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.pgc.BangumiSeason
import com.aritxonly.bdfun.core.model.video.PlayInfo
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.infra.bili.mapper.mapBangumiSeason
import com.aritxonly.bdfun.infra.bili.mapper.mapPgcVideoDetail
import com.aritxonly.bdfun.infra.bili.mapper.readProgressiveFallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val pgcPlayReferer = "https://www.bilibili.com/bangumi/play/"

/**
 * 番剧详情。信封是 `result` 不是 `data`，所以走 rawRequest 拿整包。
 *
 * 既有 season_id 也有 ep_id 入口：视频页拿到的是 ep_id（换集后也只有一个 ep_id），
 * 不能为了拿整季列表再额外查一次 season。
 */
internal suspend fun BiliApiContext.pgcSeason(seasonId: Long): BangumiSeason =
    pgcSeasonEnvelope(mapOf("season_id" to seasonId.toString()))

internal suspend fun BiliApiContext.pgcSeasonByEpisode(epId: Long): BangumiSeason =
    pgcSeasonEnvelope(mapOf("ep_id" to epId.toString()))

private suspend fun BiliApiContext.pgcSeasonEnvelope(params: Map<String, String>): BangumiSeason =
    withContext(Dispatchers.IO) {
        val root = rawRequest("/pgc/view/web/season", params, referer = pgcPlayReferer)
        val code = root.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
        val result = root.optJSONObject("result")
            ?: throw BiliException(code, "番剧信息缺失")
        mapBangumiSeason(result)
    }

/** PGC 视频详情：以 ep_id 拿整季，再把当前集投影成视频页模型（含 epId 供评论使用）。 */
internal suspend fun BiliApiContext.pgcVideoDetail(epId: Long, seasonId: Long = 0L): VideoDetail {
    // 从番剧卡片进来时只有 season_id：一次请求同时拿到剧集列表和首播/续看目标，
    // 不再先查 season 再按 bvid 查一次 video（柯南这类上千集的老番会重复拉整季）。
    if (epId <= 0L && seasonId <= 0L) throw BiliException(-400, "缺少番剧标识")
    val season = if (epId > 0L) pgcSeasonByEpisode(epId) else pgcSeason(seasonId)
    val episode = season.episodes.firstOrNull { it.epId == epId }
        ?: season.episodes.firstOrNull { it.epId == season.userStatus.lastEpId }
        ?: season.episodes.firstOrNull { it.sectionTitle == "正片" && it.cid > 0L }
        ?: season.episodes.firstOrNull()
        ?: throw BiliException(-404, "该剧集没有可播放的剧集")
    return mapPgcVideoDetail(season, episode, ::cleanBiliHtml)
}

/** PGC 播放地址：`fnval=4048` 要 DASH；视频页沿用普通清晰度/编码/CDN 选择，只替换接口与鉴权参数。 */
internal suspend fun BiliApiContext.pgcPlayInfo(
    detail: VideoDetail,
    preferredQuality: Int,
    cid: Long = detail.cid,
    durationSec: Int = detail.duration,
    codecPreference: String = "auto",
    cdnService: String = "base",
): PlayInfo = withContext(Dispatchers.IO) {
    val params = mapOf(
        "ep_id" to detail.epId.toString(),
        "cid" to cid.toString(),
        "avid" to detail.aid.toString(),
        "qn" to "127",
        "fnval" to "4048",
        "fnver" to "0",
        "fourk" to "1",
    )
    val parsed = parsePlayInfo(pgcStreamResult(params), preferredQuality, detail, durationSec, codecPreference, cdnService)
    if (parsed.fallbackUrl != null || parsed.qualities.isEmpty()) return@withContext parsed
    // DASH 解析失败时用同一 PGC 接口退回渐进式；不能拿 bvid 走普通视频接口，番剧会 404/低清。
    var fallbackUrl = ""
    for (qn in listOf("80", "64", "32")) {
        val result = cancellableCatching {
            pgcStreamResult(params + mapOf("qn" to qn, "fnval" to "1"))
        }.getOrNull() ?: continue
        val candidate = readProgressiveFallback(result)
        if (candidate.isNotBlank()) {
            fallbackUrl = candidate
            break
        }
    }
    if (fallbackUrl.isNotBlank()) parsed.copy(fallbackUrl = fallbackUrl) else parsed
}

private suspend fun BiliApiContext.pgcStreamResult(params: Map<String, String>): JSONObject {
    val root = rawRequest("/pgc/player/web/playurl", params, referer = pgcPlayReferer)
    val code = root.optInt("code", 0)
    if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
    return root.optJSONObject("data") ?: root.optJSONObject("result")
        ?: throw BiliException(code, "播放地址缺失")
}
