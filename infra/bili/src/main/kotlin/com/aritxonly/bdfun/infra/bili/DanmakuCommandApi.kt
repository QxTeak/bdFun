package com.aritxonly.bdfun.infra.bili

import com.android.purebilibili.danmaku.parser.DanmakuProto
import com.aritxonly.bdfun.core.domain.DanmakuCommandSelection
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

internal suspend fun BiliApiContext.danmakuCommands(aid: Long, cid: Long): List<DanmakuCommand> = withContext(Dispatchers.IO) {
    danmakuView(aid, cid).commandDms.mapNotNull(::mapDanmakuCommand).sortedBy { it.startMs }
}

internal suspend fun BiliApiContext.danmakuView(aid: Long, cid: Long): DanmakuProto.DmWebViewReply {
    val request = Request.Builder().url("$BILI_API/x/v2/dm/web/view?type=1&oid=$cid&pid=$aid")
        .header("User-Agent", BILI_UA).header("Referer", "https://www.bilibili.com/").build()
    val bytes = http.await(request).use {
        check(it.isSuccessful) { "弹幕元数据请求失败（${it.code}）" }
        it.body?.bytes() ?: error("弹幕元数据为空")
    }
    return DanmakuProto.parseWebViewReply(bytes)
}

/**
 * 互动投票的实时票数。命令弹幕本身只带选项文案，比例要投完票后从 vote_info 取。
 * 字段对齐 PiliPlus `VoteInfo`：`vote_info.options[].opt_idx/cnt`。
 */
internal suspend fun BiliApiContext.danmakuVoteCounts(voteId: Long): Map<Int, Long> = withContext(Dispatchers.IO) {
    if (voteId <= 0) return@withContext emptyMap()
    val data = dataRequest("/x/vote/vote_info", mapOf("vote_id" to voteId.toString()))
    val options = data.optJSONObject("vote_info")?.optJSONArray("options") ?: return@withContext emptyMap()
    buildMap {
        for (index in 0 until options.length()) {
            val row = options.optJSONObject(index) ?: continue
            val value = row.optInt("opt_idx", index + 1)
            val count = row.optLong("cnt", 0L)
            if (value > 0) put(value, count.coerceAtLeast(0L))
        }
    }
}

internal suspend fun BiliApiContext.submitDanmakuCommand(selection: DanmakuCommandSelection): Unit = withContext(Dispatchers.IO) {
    require(selection.valid)
    val command = selection.command
    if (command.kind == DanmakuCommandKind.Grade) {
        postRequest("/x/v2/dm/command/grade/post", mapOf(
            "aid" to selection.aid.toString(), "cid" to selection.cid.toString(),
            "progress" to command.startMs.toString(), "grade_id" to command.interactionId.toString(),
            "grade_score" to selection.value.toString(), "csrf" to csrf,
        ))
    } else {
        submitDanmakuVote(command.interactionId, selection.value)
    }
}

private suspend fun BiliApiContext.submitDanmakuVote(voteId: Long, option: Int) {
    if (csrf.isBlank()) throw BiliException(-101, "请先登录")
    val body = JSONObject().put("vote_id", voteId).put("votes", JSONArray(listOf(option)))
        .put("voter_uid", mid).put("status", 0).put("dynamic_id", 0)
        .put("csrf", csrf).put("csrf_token", csrf)
    val request = Request.Builder().url("$BILI_API/x/vote/do_vote?csrf=$csrf")
        .header("User-Agent", BILI_UA).header("Referer", "https://www.bilibili.com/")
        .post(body.toString().toRequestBody("application/json".toMediaType())).build()
    http.await(request).use {
        val root = JSONObject(it.body?.string().orEmpty())
        val code = root.optInt("code", -1)
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
    }
}

internal fun mapDanmakuCommand(source: DanmakuProto.CommandDm): DanmakuCommand? {
    val name = source.command.trim().uppercase()
    if (source.id <= 0 || name in setOf("UPOWER_STATE", "UPGRADE_STATE", "PANEL_STATE")) return null
    val kind = when (name) {
        "#LINK#" -> DanmakuCommandKind.Link
        "#ATTENTION#" -> DanmakuCommandKind.Follow
        "#VOTE#", "VIDEO_VOTE_MSG" -> DanmakuCommandKind.Vote
        "#GRADE#", "GRADE_MSG", "VIDEO_GRADE_MSG" -> DanmakuCommandKind.Grade
        else -> DanmakuCommandKind.Notice
    }
    val raw = listOf(source.extra, source.content).firstNotNullOfOrNull {
        runCatching { JSONObject(it) }.getOrNull()
    } ?: JSONObject()
    val vote = kind == DanmakuCommandKind.Vote
    val grade = kind == DanmakuCommandKind.Grade
    val interactionId = raw.optLong(if (grade) "grade_id" else "vote_id").takeIf { it > 0 } ?: raw.optLong("id")
    val options = if (vote || grade) mapDanmakuCommandOptions(raw.opt("options") ?: raw.opt("choices"), grade) else emptyList()
    val text = listOf("msg", "title", "question", "text", "content").firstNotNullOfOrNull {
        raw.optString(it).takeIf(String::isNotBlank)
    } ?: source.content.trim().takeIf { it.isNotBlank() && !it.startsWith("{") }
        ?: when (kind) {
            DanmakuCommandKind.Follow -> "关注 UP 主"
            DanmakuCommandKind.Grade -> "给这段打分"
            DanmakuCommandKind.Vote -> "互动投票"
            else -> return null
        }
    return DanmakuCommand(
        source.id, kind, text, source.progress.toLong().coerceAtLeast(0),
        raw.optLong("duration").takeIf { it > 0 } ?: if (vote || grade) 8000L else 3000L,
        interactionId = interactionId,
        options = if (grade && options.isEmpty()) (2..10 step 2).map { DanmakuCommandOption(it, (it / 2).toString()) } else options,
        selected = raw.optInt(if (grade) "mid_score" else "my_vote").takeIf {
            it > 0 && (!grade || (it in 2..10 && it % 2 == 0))
        },
        participants = raw.optLong("count", -1).takeIf { it >= 0 },
        average = raw.optDouble("avg_score").takeIf { it.isFinite() && it in 0.0..10.0 },
        presentation = DanmakuCommandPlacement(
            linkBvid = raw.optString("bvid").ifBlank {
                raw.optLong("aid").takeIf { it > 0 }?.let(::biliAidToBvid).orEmpty()
            },
            linkAid = raw.optLong("aid"),
        x = raw.optDouble("posX_2").takeIf(Double::isFinite)?.div(100)?.toFloat()?.coerceIn(0f, 1f),
        y = raw.optDouble("posY_2").takeIf(Double::isFinite)?.div(100)?.toFloat()?.coerceIn(0f, 1f),
            attentionType = raw.optInt("type"),
        ),
    )
}

private fun mapDanmakuCommandOptions(raw: Any?, grade: Boolean): List<DanmakuCommandOption> {
    val options = when (raw) {
        is JSONArray -> (0 until raw.length()).mapNotNull { i ->
            when (val option = raw.opt(i)) {
                is JSONObject -> {
                    val value = if (grade) option.optInt("score") else option.optInt("idx", option.optInt("opt_idx", i + 1))
                    val label = listOf("title", "name", "text", "label", "desc", "opt_desc")
                        .firstNotNullOfOrNull { option.optString(it).takeIf(String::isNotBlank) }
                        ?: if (grade) (value / 2).toString() else return@mapNotNull null
                    // 少数下发的命令已带票数；没有时投完票再走 vote_info 补全。
                    val count = listOf("cnt", "count", "votes").firstNotNullOfOrNull {
                        option.optLong(it, -1L).takeIf { value -> value >= 0 }
                    } ?: 0L
                    DanmakuCommandOption(value, label, count)
                }
                is String -> if (!grade) DanmakuCommandOption(i + 1, option) else null
                else -> null
            }
        }
        is JSONObject -> raw.keys().asSequence().mapNotNull { key ->
            key.toIntOrNull()?.let { DanmakuCommandOption(it, raw.optString(key)) }
        }.toList()
        else -> emptyList()
    }
    return options.filter { it.label.isNotBlank() && it.value > 0 && (!grade || it.value in 2..10 && it.value % 2 == 0) }
        .distinctBy { it.value }
}
