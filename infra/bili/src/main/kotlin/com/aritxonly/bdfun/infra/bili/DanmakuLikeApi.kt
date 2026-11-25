package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.DanmakuVote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 参数对齐 Hyper-PiliPlus lib/http/danmaku.dart 的 web_player 点赞。 */
internal suspend fun BiliApiContext.likeDanmaku(cid: Long, id: Long, like: Boolean): Boolean = withContext(Dispatchers.IO) {
    require(cid > 0 && id > 0)
    val csrfPresent = csrf.isNotBlank()
    if (debug) log(
        "DANMAKU_AUTH",
        "like begin cid=$cid dmid=$id op=${if (like) 1 else 2} sess=${cookieJar.hasSession} " +
            "csrf=$csrfPresent mid=${if (mid > 0) "present" else "absent"}",
    )
    try {
        postRequest("/x/v2/dm/thumbup/add", mapOf(
            "op" to if (like) "1" else "2", "dmid" to id.toString(), "oid" to cid.toString(),
            "platform" to "web_player", "polaris_app_id" to "100", "polaris_platform" to "5",
            "spmid" to "333.788.0.0", "from_spmid" to "333.788.0.0",
            "statistics" to "{\"appId\":100,\"platform\":5,\"abtest\":\"\",\"version\":\"\"}",
            "csrf" to csrf,
        ))
        if (debug) log("DANMAKU_AUTH", "like ok cid=$cid dmid=$id")
        like
    } catch (error: BiliException) {
        if (debug) log("DANMAKU_AUTH", "like fail cid=$cid dmid=$id code=${error.code} message=${error.message}")
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
    val csrfPresent = csrf.isNotBlank()
    if (debug) log(
        "DANMAKU_AUTH",
        "report begin cid=${report.cid} dmid=${report.id} reason=${report.reason.code} " +
            "sess=${cookieJar.hasSession} csrf=$csrfPresent mid=${if (mid > 0) "present" else "absent"}",
    )
    try {
        postRequest("/x/dm/report/add", mapOf(
            "cid" to report.cid.toString(), "dmid" to report.id.toString(),
            "reason" to report.reason.code.toString(), "content" to report.content,
            "block" to "false", "originCid" to report.cid.toString(),
            "polaris_app_id" to "100", "polaris_platform" to "5",
            "spmid" to "333.788.0.0", "from_spmid" to "333.788.0.0",
            "statistics" to "{\"appId\":100,\"platform\":5,\"abtest\":\"\",\"version\":\"\"}",
            "csrf" to csrf,
        ))
        if (debug) log("DANMAKU_AUTH", "report ok cid=${report.cid} dmid=${report.id}")
    } catch (error: BiliException) {
        if (debug) log("DANMAKU_AUTH", "report fail cid=${report.cid} dmid=${report.id} code=${error.code} message=${error.message}")
        throw error
    }
}
