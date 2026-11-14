package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.message.MessageItem

/** 动态的 subject_id 也可能是数字，只有明确的视频来源才能按 aid 补查。 */
internal fun messageVideoCoverTarget(item: MessageItem): String {
    if (item.bvid.isNotBlank()) return item.bvid
    val videoUri = Regex("(?:bilibili://video/|bilibili\\.com/video/)").containsMatchIn(item.nativeUri)
    if (item.businessId != 1 && !item.videoSubject && !videoUri) return ""
    return if (item.aid > 0) biliAidToBvid(item.aid) else ""
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
