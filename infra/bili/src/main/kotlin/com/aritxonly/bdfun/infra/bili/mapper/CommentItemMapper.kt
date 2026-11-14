package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.comment.*
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import org.json.JSONObject

internal fun parseComment(
    reply: JSONObject,
    upMid: Long,
    top: Boolean,
    depth: Int = 0,
): CommentItem {
    val member = reply.optJSONObject("member")
    val content = reply.optJSONObject("content")
    val mid = member?.l("mid") ?: 0
    val emotes = mutableMapOf<String, String>()
    content?.optJSONObject("emote")?.let { obj ->
        obj.keys().forEach { rawKey ->
            // 接口里的 key 是 "[doge]"，正文里是 [doge]，统一成不带括号的 "doge"
            val key = rawKey.removeSurrounding("[", "]").trim()
            val url = obj.optJSONObject(rawKey)?.s("url").orEmpty()
            if (key.isNotBlank() && url.isNotBlank()) emotes[key] = url
        }
    }
    val pictures = content?.optJSONArray("pictures")?.objects()?.mapNotNull { pic ->
        val url = pic.s("img_src").ifBlank { pic.s("url") }
        if (url.isBlank()) null else image(url, 0)
    }.orEmpty()
    // 正文里 @ 的人；官方接口会在 content.members 里给出 mid。
    val mentions = content?.optJSONArray("members")?.objects()?.mapNotNull { m ->
        val name = m.s("uname").ifBlank { m.s("name") }
        val memberMid = m.l("mid")
        if (name.isBlank() || memberMid <= 0L) null else CommentMention(memberMid, name)
    }.orEmpty()
    return CommentItem(
        rpid = reply.l("rpid"),
        uname = member?.s("uname").orEmpty(),
        avatar = avatar(member?.s("avatar").orEmpty(), 120),
        message = content?.s("message").orEmpty(),
        like = reply.optInt("like", 0),
        ctime = reply.l("ctime"),
        level = member?.optJSONObject("level_info")?.optInt("current_level") ?: 0,
        replyCount = reply.optInt("rcount", 0),
        isUp = upMid > 0 && mid == upMid,
        isTop = top,
        liked = reply.optInt("action", 0) == 1,
        extras = CommentExtras(
            mid = mid,
            oid = reply.l("oid"),
            root = reply.l("root"),
            parent = reply.l("parent"),
            emotes = emotes,
            ipLocation = reply.optJSONObject("reply_control")?.s("location").orEmpty(),
            isVip = member?.optJSONObject("vip")?.optInt("vipStatus", 0) == 1,
            pictures = pictures,
            mentions = mentions,
            previewReplies = if (depth < 1) {
            reply.optJSONArray("replies")?.objects()
                ?.take(3)
                ?.map { parseComment(it, upMid, false, depth + 1) }
                .orEmpty()
        } else {
            emptyList()
        },
        ),
    )
}
