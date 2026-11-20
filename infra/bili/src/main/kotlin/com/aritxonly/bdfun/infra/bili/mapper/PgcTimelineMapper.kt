package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.common.biliImageThumbUrl
import com.aritxonly.bdfun.core.model.pgc.BangumiCard
import org.json.JSONArray
import org.json.JSONObject

private val pgcTitlePunctuation = Regex("[\\s（）()【】\\[\\]·:：,，.。\\-—_/]")

/**
 * 番剧时间表 `/pgc/web/timeline` 的 `result` 数组 → 头图卡片。
 *
 * 时间表按天分组，每天 `episodes[]` 里是正在更新的番。头图要「比较新的番剧」，
 * 所以按 `pub_ts` 离当前时间由近到远排，同一部番只留最近更新的一集。
 * 真实字段见 2026-11-15 抓取的 `result[].episodes[]`：cover / ep_cover / pub_index / pub_ts / season_id。
 */
internal fun mapPgcTimeline(days: JSONArray, nowSec: Long): List<BangumiCard> {
    val bySeries = LinkedHashMap<String, Pair<BangumiCard, Long>>()
    for (dayIndex in 0 until days.length()) {
        val day = days.optJSONObject(dayIndex) ?: continue
        val episodes = day.optJSONArray("episodes") ?: continue
        for (index in 0 until episodes.length()) {
            val episode = episodes.optJSONObject(index) ?: continue
            val seasonId = episode.optLong("season_id", 0L)
            if (seasonId <= 0L) continue
            val title = episode.optString("title").orEmpty()
            if (title.isBlank()) continue
            val image = episode.optString("ep_cover").ifBlank {
                episode.optString("cover")
            }
            if (image.isBlank()) continue
            val pubTs = episode.optLong("pub_ts", 0L)
            val distance = if (pubTs > 0L) kotlin.math.abs(pubTs - nowSec) else Long.MAX_VALUE
            val card = BangumiCard(
                seasonId = seasonId,
                title = title,
                cover = biliImageThumbUrl(image, 960),
                // 头图只展示「整部番剧 + 简介」，不把某一话的更新文案带出去。
                indexShow = "",
                followText = episode.optString("follows").orEmpty().takeIf { it != "-" }.orEmpty(),
            )
            // 同一部番的「中文配音 / 英文 / 中配」会以不同 season_id 各占一条，按规范化标题合并。
            val key = normalizeSeriesTitle(title)
            val existing = bySeries[key]
            if (existing == null || distance < existing.second) {
                bySeries[key] = card to distance
            }
        }
    }
    return bySeries.values.sortedBy { it.second }.map { it.first }
}

/** 去掉语言/配音后缀与标点，只用于头图去重，不参与展示。 */
internal fun normalizeSeriesTitle(title: String): String {
    var value = title.lowercase()
    for (suffix in listOf(
        "中文配音", "中配", "国语", "日语", "日配", "粤语", "english", "英文",
    )) {
        value = value.replace(suffix, "")
    }
    return value.replace(pgcTitlePunctuation, "")
}

/** 单天数组的便捷入口，便于测试边界。 */
internal fun mapPgcTimeline(payload: JSONObject, nowSec: Long): List<BangumiCard> =
    mapPgcTimeline(payload.optJSONArray("result") ?: JSONArray(), nowSec)
