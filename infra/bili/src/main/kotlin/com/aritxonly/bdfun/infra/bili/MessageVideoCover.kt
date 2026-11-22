package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.message.MessageItem

/** 只有明确的视频来源才补封面：动态评论（businessId=17）之类不猜。 */
private fun messagePointsAtVideo(item: MessageItem): Boolean {
    val videoUri = Regex("(?:bilibili://video/|bilibili\\.com/video/)").containsMatchIn(item.nativeUri)
    return item.bvid.isNotBlank() || videoUri ||
        item.businessId == 1 || item.videoSubject || item.aid > 0 && videoUri
}

/** 消息里明确带出来的 BV：native_uri 或 detail.bvid。 */
internal fun messageVideoExplicitBvid(item: MessageItem): String =
    Regex("BV[0-9A-Za-z]{10}").find(item.nativeUri)?.value.orEmpty().ifBlank { item.bvid }

/** 消息只给 aid 时，用 view?aid= 让服务器换 BV（64 位新 av 号不能本地换算）。 */
internal fun messageVideoCoverAid(item: MessageItem): Long =
    if (messagePointsAtVideo(item)) item.aid.takeIf { it > 0L } ?: 0L else 0L

/** 只按消息自带信息就能确定的目标：优先真 BV，否则退回 aid。 */
internal fun messageVideoCoverTarget(item: MessageItem): String {
    if (!messagePointsAtVideo(item)) return ""
    val explicit = messageVideoExplicitBvid(item)
    if (explicit.isNotBlank()) return explicit
    return if (item.aid > 0L) biliAidToBvid(item.aid) else ""
}

internal fun applyMessageVideoBrief(item: MessageItem, bvid: String, cover: String, ownerMid: Long): MessageItem =
    item.copy(
        navigation = item.navigation.copy(bvid = item.bvid.ifBlank { bvid }),
        presentation = item.presentation.copy(
            originalVideoCover = cover,
            subjectCover = if (item.videoSubject && item.subjectCover.isBlank()) cover else item.subjectCover,
            isUp = item.isUp || item.userMid > 0 && ownerMid > 0 && item.userMid == ownerMid,
        ),
    )
