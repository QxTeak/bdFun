package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.model.message.MessageItem

internal sealed interface MessageOpenTarget {
    data object Feed : MessageOpenTarget
    data object MissingVideo : MessageOpenTarget
    data class Video(val rootRpid: Long, val targetRpid: Long) : MessageOpenTarget
}

/** 保留原消息入口的业务分支；nativeUri 的 Android 解析仍由导航适配层负责。 */
internal fun messageOpenTarget(item: MessageItem, kind: MessageFeedKind): MessageOpenTarget {
    // 旧实现允许普通视频 aid 覆盖 businessId，避免有效稿件被归入动态分支。
    val looksLikeVideo = item.aid in 1L..9_999_999_999L
    if (item.businessId != 0 && item.businessId != 1 && !looksLikeVideo) return MessageOpenTarget.Feed
    if (item.aid <= 0L && item.bvid.isBlank()) return MessageOpenTarget.MissingVideo
    val focus = if (kind == MessageFeedKind.Like) {
        item.targetRpid.takeIf { it > 0L } ?: item.sourceRpid
    } else {
        item.targetRpid.takeIf { it > 0L }
            ?: item.sourceRpid.takeIf { it > 0L }
            ?: item.rootRpid
    }
    val root = item.rootRpid.takeIf { it > 0L }
        ?: item.targetRpid.takeIf { it > 0L }
        ?: focus
    return MessageOpenTarget.Video(root, focus)
}
