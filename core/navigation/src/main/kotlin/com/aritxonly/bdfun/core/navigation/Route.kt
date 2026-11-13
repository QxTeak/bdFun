package com.aritxonly.bdfun.core.navigation

import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.model.feed.FeedItem

sealed interface Route {
    data class MessageInbox(val pageType: Int, val title: String) : Route
    data class MessageTools(val kind: String, val id: String = "", val type: Int = 0, val count: Int = 0,
        val navigation: com.aritxonly.bdfun.core.model.message.MessageNavigation = com.aritxonly.bdfun.core.model.message.MessageNavigation()) : Route
    data class Video(
        val bvid: String, val aid: Long = 0, val snapshot: VideoItem? = null,
        val fromVideoPage: Boolean = false,
        val commentRoot: Long = 0, val commentTarget: Long = 0, val seekMs: Long = 0,
    ) : Route
    data class Space(val mid: Long) : Route
    data class Feed(val id: String, val snapshot: FeedItem? = null) : Route
    data class PrivateChat(
        val talkerId: Long, val sessionType: Int = 1, val name: String = "", val avatar: String = "",
        val unread: Int = 0, val maxSeqno: Long = 0,
    ) : Route
}
