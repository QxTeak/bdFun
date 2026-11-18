package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 参数对齐 Hyper-PiliPlus lib/http/danmaku.dart 的 web_player 点赞。 */
internal suspend fun BiliApiContext.likeDanmaku(cid: Long, id: Long): Unit = withContext(Dispatchers.IO) {
    require(cid > 0 && id > 0)
    postRequest("/x/v2/dm/thumbup/add", mapOf(
        "op" to "1", "dmid" to id.toString(), "oid" to cid.toString(),
        "platform" to "web_player", "polaris_app_id" to "100", "polaris_platform" to "5",
        "spmid" to "333.788.0.0", "from_spmid" to "333.788.0.0",
        "statistics" to "{\"appId\":100,\"platform\":5,\"abtest\":\"\",\"version\":\"\"}",
        "csrf" to csrf,
    ))
    Unit
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
