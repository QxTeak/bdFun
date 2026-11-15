package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.common.biliImageThumbUrl
import com.aritxonly.bdfun.core.model.pgc.BangumiEpisode
import com.aritxonly.bdfun.core.model.pgc.BangumiSeason
import com.aritxonly.bdfun.core.model.pgc.BangumiStream
import org.json.JSONObject

/**
 * `/pgc/view/web/season` 的 result → BangumiSeason。
 *
 * 字段来自 2026-11-14 的真实响应：episodes[].ep_id/aid/cid/title/long_title/cover/duration/index，
 * 预告花絮在 sections[]，每项有 title + episodes。
 */
internal fun mapBangumiSeason(result: JSONObject): BangumiSeason {
    val main = result.optJSONArray("episodes").objects().mapNotNull { it.toEpisode("正片") }
    val extra = ArrayList<BangumiEpisode>()
    result.optJSONArray("sections").objects().forEach { section ->
        val title = section.s("title").ifBlank { "其他" }
        section.optJSONArray("episodes").objects().forEach { item ->
            item.toEpisode(title)?.let(extra::add)
        }
    }
    return BangumiSeason(
        seasonId = result.l("season_id"),
        title = result.s("title"),
        cover = biliImageThumbUrl(result.s("cover"), 480),
        rating = result.optJSONObject("rating")?.s("score").orEmpty(),
        evaluate = result.s("evaluate"),
        subtitle = result.s("subtitle"),
        episodes = dedupeById(main + extra),
    )
}

/** 同一集可能同时出现在 episodes 和 sections 里，按 ep_id 保留第一次出现。 */
private fun dedupeById(items: List<BangumiEpisode>): List<BangumiEpisode> {
    val seen = HashSet<Long>()
    return items.filter { it.epId > 0L && seen.add(it.epId) }
}

private fun JSONObject.toEpisode(sectionTitle: String): BangumiEpisode? {
    val epId = l("ep_id").takeIf { it > 0L } ?: l("id").takeIf { it > 0L } ?: return null
    val cid = l("cid")
    if (cid <= 0L) return null
    return BangumiEpisode(
        epId = epId,
        aid = l("aid"),
        cid = cid,
        bvid = s("bvid"),
        title = s("title"),
        longTitle = s("long_title"),
        cover = biliImageThumbUrl(s("cover"), 320),
        index = s("title"),
        durationMs = l("duration"),
        sectionTitle = sectionTitle,
    )
}

/**
 * `/pgc/player/web/playurl` 的 result → BangumiStream。
 *
 * DASH：dash.video[] 与 dash.audio[] 分开，各自有 base_url。
 * 老接口（fnval=0）会给 durl[]，这里一并兼容。
 */
internal fun mapBangumiStream(result: JSONObject): BangumiStream? {
    val durationMs = result.l("timelength")
    val dash = result.optJSONObject("dash")
    if (dash != null) {
        val video = dash.optJSONArray("video").objects()
            .filter { it.s("baseUrl").ifBlank { it.s("base_url") }.isNotBlank() }
        if (video.isEmpty()) return null
        // 同一 id 会按编码给多条（avc/hevc/av1），取第一条即可，编码兼容性交给播放器。
        val best = video.maxByOrNull { it.optInt("id") } ?: return null
        val audio = dash.optJSONArray("audio").objects()
            .filter { it.s("baseUrl").ifBlank { it.s("base_url") }.isNotBlank() }
            .maxByOrNull { it.l("bandwidth") }
        return BangumiStream(
            videoUrl = best.s("baseUrl").ifBlank { best.s("base_url") },
            audioUrl = audio?.let { it.s("baseUrl").ifBlank { it.s("base_url") } },
            qualityId = best.optInt("id"),
            qualityLabel = qualityLabel(best.optInt("id")),
            durationMs = durationMs,
        )
    }
    val durl = result.optJSONArray("durl").objects().firstOrNull() ?: return null
    val url = durl.s("url").ifBlank { durl.s("backup_url") }
    if (url.isBlank()) return null
    return BangumiStream(
        videoUrl = url,
        audioUrl = null,
        qualityId = result.optInt("quality"),
        qualityLabel = qualityLabel(result.optInt("quality")),
        durationMs = durationMs,
    )
}

private fun qualityLabel(id: Int): String = when (id) {
    127 -> "8K"
    126 -> "杜比视界"
    125 -> "HDR"
    120 -> "4K"
    116 -> "1080P60"
    112 -> "1080P+"
    80 -> "1080P"
    74 -> "720P60"
    64 -> "720P"
    32 -> "480P"
    16 -> "360P"
    else -> "${id}P"
}
