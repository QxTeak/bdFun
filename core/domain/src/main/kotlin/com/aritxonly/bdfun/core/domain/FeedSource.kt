package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.feed.FeedPage
import com.aritxonly.bdfun.core.model.feed.FeedPortal
import com.aritxonly.bdfun.core.model.feed.FeedUpPage

/** Service contract verified against Hyper-PiliPlus 0d08dd25 (2026-11-11).
 * Keep server ordering and cursors; the UP feed is not the public space feed.
 */
const val DYNAMIC_FEED_PATH = "/x/polymer/web-dynamic/v1/feed/all"
const val DYNAMIC_PORTAL_PATH = "/x/polymer/web-dynamic/v1/portal"
const val DYNAMIC_UP_LIST_PATH = "/x/polymer/web-dynamic/v1/uplist"
const val DYNAMIC_FEED_FEATURES = "itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,decorationCard,onlyfansAssetsV2,forwardListHidden,ugcDelete,onlyfansQaCard,commentsNewVersion,avatarAutoTheme,sunflowerStyle,cardsEnhance,eva3CardOpus,eva3CardVideo,eva3CardComment,eva3CardVote,eva3CardUser"
val dynamicFeedTypes = listOf("all", "video", "pgc", "article", "up")
val dynamicFeedLabels = listOf("全部", "投稿", "番剧", "专栏", "UP")
val dynamicPortalParams = mapOf("up_list_more" to "1", "web_location" to "333.1365")

fun dynamicFeedParams(type: String, offset: String, hostMid: Long?): Map<String, String> {
    require(type in dynamicFeedTypes)
    return buildMap {
        if (type == "up") {
            require(hostMid != null && hostMid > 0)
            put("host_mid", hostMid.toString())
        } else put("type", type)
        if (offset.isNotEmpty()) put("offset", offset)
        put("features", DYNAMIC_FEED_FEATURES)
    }
}

fun dynamicUpListParams(offset: String) = mapOf(
    "offset" to offset, "platform" to "web", "web_location" to "333.1365",
)


interface FeedSource {
    suspend fun dynamicPage(type: String, offset: String = "", hostMid: Long? = null): FeedPage
    suspend fun dynamicPortal(): FeedPortal
    suspend fun dynamicUpList(offset: String): FeedUpPage
}
