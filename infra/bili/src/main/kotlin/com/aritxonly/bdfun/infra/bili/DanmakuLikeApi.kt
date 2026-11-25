package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.DanmakuVote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 参数对齐 Hyper-PiliPlus lib/http/danmaku.dart 的 web_player 点赞。 */
internal suspend fun BiliApiContext.likeDanmaku(cid: Long, id: Long, like: Boolean): Boolean = withContext(Dispatchers.IO) {
    require(cid > 0 && id > 0)
    try {
        postRequest("/x/v2/dm/thumbup/add", mapOf(
            "op" to if (like) "1" else "2", "dmid" to id.toString(), "oid" to cid.toString(),
            "platform" to "web_player", "polaris_app_id" to "100", "polaris_platform" to "5",
            "spmid" to "333.788.0.0", "from_spmid" to "333.788.0.0",
            "statistics" to "{\"appId\":100,\"platform\":5,\"abtest\":\"\",\"version\":\"\"}",
            "csrf" to csrf,
        ))
        like
    } catch (error: BiliException) {
        when (error.code) {
            65006 -> true
            65004 -> false
            else -> throw error
        }
    }
}

internal suspend fun BiliApiContext.danmakuVotes(cid: Long, ids: List<Long>): Map<Long, DanmakuVote> =
    withContext(Dispatchers.IO) {
        val data = dataRequest("/x/v2/dm/thumbup/stats",
            mapOf("oid" to cid.toString(), "ids" to ids.joinToString(",")))
        buildMap {
            for (id in ids) {
                val row = data.optJSONObject(id.toString()) ?: continue
                val count = row.optLong("likes", -1L).takeIf { it >= 0 }
                put(id, DanmakuVote(count, row.optInt("user_like") == 1))
            }
        }
    }

internal suspend fun BiliApiContext.reportDanmaku(report: com.aritxonly.bdfun.core.domain.DanmakuReport): Unit = withContext(Dispatchers.IO) {
    require(report.valid)
    postRequest("/x/dm/report/add", mapOf(
        "cid" to report.cid.toString(), "dmid" to report.id.toString(),
        "reason" to report.reason.code.toString(), "content" to report.content,
        "block" to "false", "originCid" to report.cid.toString(),
        "polaris_app_id" to "100", "polaris_platform" to "5",
        "spmid" to "333.788.0.0", "from_spmid" to "333.788.0.0",
        "statistics" to "{\"appId\":100,\"platform\":5,\"abtest\":\"\",\"version\":\"\"}",
        "csrf" to csrf,
    ))
    Unit
}
