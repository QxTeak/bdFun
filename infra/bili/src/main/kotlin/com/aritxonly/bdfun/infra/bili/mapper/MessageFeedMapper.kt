package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.message.MessagePresentation
import com.aritxonly.bdfun.core.model.message.MessageNotice
import com.aritxonly.bdfun.core.model.message.MessageNavigation
import com.aritxonly.bdfun.core.model.message.MessageActor
import com.aritxonly.bdfun.core.model.message.MessageItem
import org.json.JSONObject
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar

/** 仅包装旧消息响应；字段和回退表达式按原解析顺序保留，不进行网络请求。 */
internal class MessageFeedItemDto(val type: Int, val item: JSONObject) {
    val detail = item.optJSONObject("item")
        // 收到的赞把用户放在 users 数组里，其余类型是单个 user 对象
    val users = item.optJSONArray("users").objects()
    val user = item.optJSONObject("user") ?: users.firstOrNull()
    val names = users.mapNotNull {
            it.s("nickname").ifBlank { it.s("uname") }.takeIf(String::isNotBlank)
        }
    val detailType = detail?.s("type").orEmpty()
    val isLike = type == 2
    val isAt = type == 1 || detailType == "at"
    val sourceContent = detail?.s("source_content").orEmpty()
    val targetContent = detail?.s("target_reply_content").orEmpty()
    val rootContent = detail?.s("root_reply_content").orEmpty()
    val businessId = detail?.optInt("business_id", 0) ?: 0
    val rawContent = item.s("content")
            .ifBlank { detail?.s("content").orEmpty() }
            .ifBlank { detail?.s("message").orEmpty() }
        // 回复通知的正文经常只放在 source_content 里；点赞通知优先保留原始正文，
        // 这样「收到喜欢」右侧才能显示被点赞的评论原文。
}

private data class MessageFeedPresentation(
    val content: String,
    val likeVideo: Boolean,
    val commentOnMyVideo: Boolean,
    val action: String,
    val subjectText: String,
    val quote: String,
    val source: String,
    val title: String,
    val subjectCover: String,
    val videoSubject: Boolean,
)

private fun MessageFeedItemDto.presentation(): MessageFeedPresentation {
    val content = if (isLike) rawContent else rawContent.ifBlank { sourceContent }
    val likeVideo = isLike && (
        detailType == "video" || detailType == "archive" ||
            (businessId == 1 && sourceContent.isBlank() &&
                targetContent.isBlank() && rootContent.isBlank() && rawContent.isBlank())
        )
    val commentOnMyVideo = !isLike && !isAt &&
        (detailType == "video" ||
            businessId == 1 && targetContent.isBlank() && rootContent.isBlank())
    val business = detail?.s("business").orEmpty()
    val action = when {
        isLike && business.isNotBlank() -> "赞了我的$business"
        isAt && business.isNotBlank() -> "在${business}中@了我"
        !isLike && !isAt && business.isNotBlank() ->
            (if (item.optInt("is_multi") == 1) "等人 " else "") + "对我的${business}发布了${item.optInt("counts", 1)}条评论"
        isLike && likeVideo -> "赞了我的视频"
        isLike -> "赞了我的评论"
        isAt -> "提到了我"
        commentOnMyVideo -> "对我的视频发表了评论"
        else -> "回复了我的评论"
    }
    // 右侧：层主评论 > 我原来的评论；评论我视频时固定用视频封面。
    val subjectText = when {
        commentOnMyVideo -> ""
        isLike -> if (likeVideo) "" else sourceContent
            .ifBlank { targetContent }
            .ifBlank { rootContent }
            .ifBlank { rawContent }
        rootContent.isNotBlank() -> rootContent
        targetContent.isNotBlank() -> targetContent
        else -> ""
    }
    // 底部引用只在线程里出现：我在这层楼里说过的话。
    val quote = if (!isLike && rootContent.isNotBlank()) targetContent else ""
    val source = sourceContent
        .ifBlank { rootContent }
        .ifBlank { targetContent }
        .ifBlank { rawContent }
        .ifBlank { detail?.s("desc").orEmpty() }
        .ifBlank { item.s("title") }
    val title = detail?.s("title").orEmpty().ifBlank { item.s("title") }
    val subjectCover = if (isLike || isAt || commentOnMyVideo) {
        detail?.s("image").orEmpty()
            .ifBlank { detail?.s("cover").orEmpty() }
            .ifBlank { detail?.s("pic").orEmpty() }
    } else {
        ""
    }
    val videoSubject = likeVideo || commentOnMyVideo
    return MessageFeedPresentation(content, likeVideo, commentOnMyVideo, action, subjectText, quote, source, title, subjectCover, videoSubject)
}

private data class MessageFeedTarget(
    val aid: Long,
    val bvid: String,
    val nativeUri: String,
    val uriRootRpid: Long,
    val uriSecondRpid: Long,
    val seekSeconds: Long,
)

private val uriAidPatterns = listOf(
    Regex("bilibili://video/(\\d+)"),
    Regex("bilibili://video/[aA][vV](\\d+)"),
    Regex("bilibili\\.com/video/[aA][vV](\\d+)"),
)
private val uriBvidPattern = Regex("BV[0-9A-Za-z]{10}")
private val uriRootRpidPattern = Regex("[?&]comment_root_id=(\\d+)")
private val uriSecondRpidPattern = Regex("[?&]comment_secondary_id=(\\d+)")
private val uriSeekPattern = Regex("[?&#](?:t|start|time)=([0-9]+)")

private fun MessageFeedItemDto.target(): MessageFeedTarget {
    val subjectId = (detail?.l("subject_id") ?: 0L)
        .takeIf { it > 0L }
        ?: item.l("subject_id")
    val nativeUri = detail?.s("native_uri").orEmpty()
        .ifBlank { detail?.s("uri").orEmpty() }
        .ifBlank { item.s("native_uri").orEmpty() }
    // 消息接口最可靠的是 native_uri：bilibili://video/{aid}/?comment_root_id=..&comment_secondary_id=..
    // 之前只看 bvid / subject_id，回复消息里这两项经常为空或是别的东西，导致跳过去是空的。
    val uriAid = uriAidPatterns.firstNotNullOfOrNull { pattern ->
        pattern.find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }
    val uriBvid = uriBvidPattern.find(nativeUri)?.value.orEmpty()
    val uriRootRpid = uriRootRpidPattern
        .find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    val uriSecondRpid = uriSecondRpidPattern
        .find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    val bvid = uriBvid.ifBlank {
        detail?.s("bvid").orEmpty()
    }
    val aid = when {
        uriAid != null && uriAid > 0L -> uriAid
        subjectId > 0L -> subjectId
        else -> (detail?.l("oid") ?: 0L).takeIf { it > 0L }
            ?: (item.l("oid") ?: 0L)
    }
    val seekSeconds = uriSeekPattern
        .find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    return MessageFeedTarget(aid, bvid, nativeUri, uriRootRpid, uriSecondRpid, seekSeconds)
}

internal fun mapMessageFeedItem(payload: MessageFeedItemDto): MessageItem? = with(payload) {
    val (content, likeVideo, commentOnMyVideo, action, subjectText, quote, source, title, subjectCover, videoSubject) = presentation()
    val (aid, bvid, nativeUri, uriRootRpid, uriSecondRpid, seekSeconds) = target()
    // 点赞通知的文字字段经常是空的，只要有用户就保留，否则整页会「没有内容」。
    if (type != 2 && title.isBlank() && content.isBlank() && source.isBlank()) {
        return@with null
    }
    return@with MessageItem(
        id = item.s("id").ifBlank { "${type}_${item.l("id")}" },
        title = title,
        content = content,
        time = item.l("reply_time")
            .takeIf { it > 0 }
            ?: item.l("at_time").takeIf { it > 0 }
            ?: item.l("ctime").takeIf { it > 0 }
            ?: item.l("timestamp").takeIf { it > 0 }
            ?: item.l("like_time").takeIf { it > 0 }
            ?: (item.s("pub_time").toLongOrNull() ?: 0L),
        name = user?.s("nickname").orEmpty().ifBlank { user?.s("uname").orEmpty() },
        avatar = avatar(user?.s("avatar").orEmpty(), 96),
        navigation = MessageNavigation(
            bvid = bvid,
            aid = aid,
            rootRpid = uriRootRpid.takeIf { it > 0L } ?: (detail?.l("root_id") ?: 0L),
            targetRpid = uriSecondRpid.takeIf { it > 0L } ?: (detail?.l("target_id") ?: 0L),
            sourceRpid = uriSecondRpid.takeIf { it > 0L }
            ?: (detail?.l("source_id") ?: 0L),
            businessId = businessId,
            seekMs = seekSeconds * 1000L,
            nativeUri = nativeUri,
        ),
        presentation = MessagePresentation(
            action = action,
            source = source,
            quote = quote,
            subjectText = subjectText,
            subjectCover = subjectCover,
            videoSubject = videoSubject,
            atMe = isAt,
            commentOnMyVideo = commentOnMyVideo,
        ),
        actor = MessageActor(
            userMid = user?.l("mid") ?: 0L,
            followed = isFollowedMessageUser(user, item),
            likeNames = names,
            likeAvatars = users.take(4).map { avatar(it.s("avatar"), 96) },
            likeCount = maxOf(
            item.optInt("counts", 0),
            detail?.optInt("counts", 0) ?: 0,
            names.size,
        ),
        ),
        notice = MessageNotice(
            noticeState = item.optInt("notice_state", 0),
        ),
    )
}

private fun isFollowedMessageUser(user: JSONObject?, item: JSONObject): Boolean {
    fun followed(obj: JSONObject?): Boolean {
        if (obj == null) return false
        if (obj.optBoolean("follow", false)) return true
        if (obj.optBoolean("is_follow", false)) return true
        if (obj.optInt("is_follow", 0) > 0) return true
        if (obj.optInt("follow_status", 0) > 0) return true
        val attribute = obj.optInt("attribute", 0)
        // 1=悄悄关注 / 2=已关注 / 6=互关，其余（拉黑等）不算。
        return attribute == 1 || attribute == 2 || attribute == 6
    }
    return followed(user) || followed(item)
}
