package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.DanmakuCloudRule
import com.aritxonly.bdfun.core.domain.DanmakuRuleKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal suspend fun BiliApiContext.recallDanmaku(cid: Long, id: Long): Unit = withContext(Dispatchers.IO) {
    postRequest("/x/dm/recall", mapOf(
        "cid" to cid.toString(), "dmid" to id.toString(), "csrf" to csrf, "type" to "1",
    ))
}

internal suspend fun BiliApiContext.danmakuRules(): List<DanmakuCloudRule> = withContext(Dispatchers.IO) {
    val data = dataRequest("/x/dm/filter/user", emptyMap())
    buildList {
        for (key in listOf("rule", "rule1", "rule2")) {
            val rows = data.optJSONArray(key) ?: continue
            for (i in 0 until rows.length()) rows.optJSONObject(i)?.toDanmakuRule()?.let(::add)
        }
    }.distinctBy { it.id }
}

internal suspend fun BiliApiContext.addDanmakuRule(kind: DanmakuRuleKind, value: String): DanmakuCloudRule =
    withContext(Dispatchers.IO) {
        postRequest("/x/dm/filter/user/add", mapOf("type" to kind.code.toString(), "filter" to value, "csrf" to csrf))
            .toDanmakuRule() ?: error("弹幕屏蔽规则返回无效")
    }

internal suspend fun BiliApiContext.deleteDanmakuRule(id: Long): Unit = withContext(Dispatchers.IO) {
    postRequest("/x/dm/filter/user/del", mapOf("ids" to id.toString(), "csrf" to csrf))
}

private fun JSONObject.toDanmakuRule(): DanmakuCloudRule? {
    val id = optLong("id")
    val kind = DanmakuRuleKind.entries.firstOrNull { it.code == optInt("type", -1) } ?: return null
    val value = optString("filter")
    return if (id > 0 && value.isNotBlank()) DanmakuCloudRule(id, kind, value) else null
}

internal suspend fun BiliApiContext.syncDanmakuConfig(config: com.aritxonly.bdfun.core.domain.DanmakuCloudConfig): Unit =
    withContext(Dispatchers.IO) {
        try {
            postRequest("/x/v2/dm/web/config", mapOf(
                "dm_switch" to config.enabled.toString(), "blockscroll" to config.allowScroll.toString(),
                "blocktop" to config.allowFixed.toString(), "blockbottom" to config.allowFixed.toString(),
                "blockcolor" to config.allowColor.toString(), "blockspecial" to config.allowSpecial.toString(),
                "opacity" to config.opacity.coerceIn(0f, 1f).toString(),
                "dmarea" to (config.area.coerceIn(0f, 1f) * 100).toInt().toString(),
                "speedplus" to config.speed.coerceIn(0.4f, 1.6f).toString(),
                "fontsize" to config.fontScale.coerceIn(0.51f, 1.6f).toString(),
                "csrf" to csrf,
            ))
        } catch (error: BiliException) {
            if (error.code != 23004) throw error
        }
    }
