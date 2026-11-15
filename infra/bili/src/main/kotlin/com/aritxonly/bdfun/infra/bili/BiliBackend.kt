package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.infra.bili.parseBiliDurationText
import com.aritxonly.bdfun.infra.bili.parseBiliCountText
import com.aritxonly.bdfun.infra.bili.cleanBiliHtml
import com.aritxonly.bdfun.infra.bili.biliQualityLabel
import com.aritxonly.bdfun.infra.bili.biliImage
import com.aritxonly.bdfun.infra.bili.biliAvatar
import com.aritxonly.bdfun.infra.bili.biliAidToBvid
import com.aritxonly.bdfun.infra.bili.BILI_VC
import com.aritxonly.bdfun.infra.bili.BILI_UA
import com.aritxonly.bdfun.infra.bili.BILI_PASSPORT
import com.aritxonly.bdfun.infra.bili.BILI_APP_UA
import com.aritxonly.bdfun.infra.bili.BILI_APP_API
import com.aritxonly.bdfun.infra.bili.BILI_APP
import com.aritxonly.bdfun.infra.bili.BILI_API
import android.content.Context
import com.aritxonly.bdfun.core.domain.FeedSource
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.comment.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.model.library.*

/** 迁移期兼容门面；业务实现位于各网络分片。 */
open class BiliBackend(context: Context, debug: Boolean, log: (String, String) -> Unit, cdnResolver: (String) -> CdnService) : FeedSource {
    internal val api = BiliApiContext(context, debug, log, cdnResolver)
    val webSocketClient get() = api.webSocketClient
    val cookieJar get() = api.cookieJar
    /** 播放器拉流用的 Cookie 头（直播 CDN 需要）。 */
    fun cookieHeader(): String = api.cookieJar.header()
    val isLoggedIn get() = api.isLoggedIn
    val mid get() = api.mid
    val csrf get() = api.csrf

    suspend fun liveRoomInfo(roomId: Long) = api.liveRoomInfo(roomId)
    suspend fun liveHotRoomId() = api.liveHotRoomId()
    suspend fun livePortraitBackground(roomId: Long) = api.livePortraitBackground(roomId)
    suspend fun liveQualities(roomId: Long) = api.liveQualities(roomId)
    suspend fun liveEmoticons(roomId: Long) = api.liveEmoticons(roomId)
    suspend fun liveDanmuInfo(roomId: Long) = api.liveDanmuInfo(roomId)
    suspend fun livePlayUrl(roomId: Long, qn: Int = 10000) = api.livePlayUrl(roomId, qn)
    // 直播页 / 番剧页（PiliPlus 同款接口）
    suspend fun liveHome(page: Int = 1) = api.liveHome(page)
    suspend fun liveRecommend(page: Int = 1, parentAreaId: Long = 0L, areaId: Long = 0L) =
        api.liveRecommend(page, parentAreaId, areaId)
    suspend fun liveAreaList(parentId: Long = 0L) = api.liveAreaList(parentId)
    suspend fun liveFollowers() = api.liveFollowers()
    suspend fun pgcFollowing(page: Int = 1) = api.pgcFollowing(page)
    suspend fun pgcIndex(page: Int = 1, pageSize: Int = 20) = api.pgcIndex(page, pageSize)
    suspend fun nav() = api.nav()
    suspend fun qrGenerate() = api.qrGenerate()
    suspend fun qrPoll(key: String) = api.qrPoll(key)
    fun logout() = api.logout()
    suspend fun recommend(freshIndex: Int = 1, web: Boolean = true) = api.recommend(freshIndex, web)
    suspend fun dislikeRecommendation(video: VideoItem, reason: RecommendationReason) = api.dislikeRecommendation(video, reason)
    suspend fun popular(page: Int = 1) = api.popular(page)
    suspend fun ranking(rid: Int = 0) = api.ranking(rid)
    suspend fun view(bvid: String) = api.view(bvid)
    suspend fun viewByAid(aid: Long) = api.viewByAid(aid)
    suspend fun playInfo( detail: VideoDetail, preferredQuality: Int, cid: Long = detail.cid, durationSec: Int = detail.duration, codecPreference: String = "auto", cdnService: String = "base", ) = api.playInfo(detail, preferredQuality, cid, durationSec, codecPreference, cdnService)
    suspend fun related(bvid: String) = api.related(bvid)
    suspend fun danmaku(cid: Long, durationSec: Int = 0) = api.danmaku(cid, durationSec)
    suspend fun sponsorSegments(bvid: String) = api.sponsorSegments(bvid)
    suspend fun comments( aid: Long, page: Int, upMid: Long = 0, sort: Int = 2, ) = api.comments(aid, page, upMid, sort)
    suspend fun commentsByOid( oid: String, type: Int, page: Int, upMid: Long = 0, sort: Int = 2, ) = api.commentsByOid(oid, type, page, upMid, sort)
    suspend fun replies(aid: Long, root: Long, page: Int = 1, upMid: Long = 0) = api.replies(aid, root, page, upMid)
    suspend fun repliesByOid( oid: String, type: Int, root: Long, page: Int = 1, upMid: Long = 0, sortTime: Boolean = false, ) = api.repliesByOid(oid, type, root, page, upMid, sortTime)
    suspend fun addComment( aid: Long, message: String, root: Long = 0, parent: Long = 0, pictures: String? = null, ) = api.addComment(aid, message, root, parent, pictures)
    suspend fun addCommentByOid( oid: String, type: Int, message: String, root: Long = 0, parent: Long = 0, pictures: String? = null, ) = api.addCommentByOid(oid, type, message, root, parent, pictures)
    suspend fun uploadCommentImage(uri: android.net.Uri) = api.uploadCommentImage(uri)
    suspend fun sendDanmaku( cid: Long, bvid: String, text: String, progressMs: Long = 0L, mode: Int = 1, fontSize: Int = 25, color: Int = 0xFFFFFF, ) = api.sendDanmaku(cid, bvid, text, progressMs, mode, fontSize, color)
    suspend fun likeComment(aid: Long, rpid: Long, like: Boolean) = api.likeComment(aid, rpid, like)
    suspend fun likeCommentByOid( oid: String, type: Int, rpid: Long, like: Boolean, ) = api.likeCommentByOid(oid, type, rpid, like)
    suspend fun reportComment(aid: Long, rpid: Long, reason: Int = 1) = api.reportComment(aid, rpid, reason)
    suspend fun reportCommentByOid( oid: String, type: Int, rpid: Long, reason: Int = 1, ) = api.reportCommentByOid(oid, type, rpid, reason)
    suspend fun deleteComment(aid: Long, rpid: Long) = api.deleteComment(aid, rpid)
    suspend fun deleteCommentByOid(oid: String, type: Int, rpid: Long) = api.deleteCommentByOid(oid, type, rpid)
    suspend fun blockUser(mid: Long) = api.blockUser(mid)
    suspend fun replyEmotes() = api.replyEmotes()
    suspend fun likeVideo(aid: Long, like: Boolean) = api.likeVideo(aid, like)
    suspend fun dislikeVideo(aid: Long, dislike: Boolean = true) = api.dislikeVideo(aid, dislike)
    suspend fun coinVideo(aid: Long, count: Int, alsoLike: Boolean) = api.coinVideo(aid, count, alsoLike)
    suspend fun triple(aid: Long) = api.triple(aid)
    suspend fun favoriteFolderIds(aid: Long) = api.favoriteFolderIds(aid)
    suspend fun setFavorite(aid: Long, folderId: Long, add: Boolean) = api.setFavorite(aid, folderId, add)
    suspend fun removeFavoriteAll(aid: Long, folderIds: Collection<Long>) = api.removeFavoriteAll(aid, folderIds)
    suspend fun addWatchLater(aid: Long) = api.addWatchLater(aid)
    suspend fun deleteHistory(aid: Long) = api.deleteHistory(aid)
    suspend fun deleteWatchLater(aid: Long) = api.deleteWatchLater(aid)
    suspend fun favoriteVideo(aid: Long, folderIds: String) = api.favoriteVideo(aid, folderIds)
    suspend fun followUser(mid: Long, follow: Boolean) = api.followUser(mid, follow)
    suspend fun relation(mid: Long) = api.relation(mid)
    suspend fun relationUsers(mid: Long, fans: Boolean, page: Int = 1, frequent: Boolean = false) = api.relationUsers(mid, fans, page, frequent)
    suspend fun history() = api.history()
    suspend fun historyPage( maxCursor: Long, viewAtCursor: Long, businessCursor: String = "", ) = api.historyPage(maxCursor, viewAtCursor, businessCursor)
    suspend fun cloudResumePositionMs( aid: Long, cid: Long, durationSec: Int = 0, ) = api.cloudResumePositionMs(aid, cid, durationSec)
    suspend fun reportHistory( aid: Long, cid: Long, progressSec: Long, ) = api.reportHistory(aid, cid, progressSec)
    suspend fun enrichHistoryStats(items: List<HistoryItem>) = api.enrichHistoryStats(items)
    suspend fun watchLater() = api.watchLater()
    suspend fun favoriteFolders() = api.favoriteFolders()
    suspend fun favoriteVideos(folderId: Long) = api.favoriteVideos(folderId)
    suspend fun favoriteVideosPage(folderId: Long, page: Int) = api.favoriteVideosPage(folderId, page)
    suspend fun userFeeds(mid: Long, offset: String = "") = api.userFeeds(mid, offset)
    suspend fun upVideoCount(mid: Long) = api.upVideoCount(mid)
    suspend fun onlineTotal(aid: Long, cid: Long) = api.onlineTotal(aid, cid)
    suspend fun relationStat(mid: Long) = api.relationStat(mid)
    suspend fun userLikes(mid: Long) = api.userLikes(mid)
    suspend fun followings() = api.followings()
    suspend fun subtitles(aid: Long, cid: Long) = api.subtitles(aid, cid)
    suspend fun subtitleCues(url: String) = api.subtitleCues(url)
    suspend fun dynamics() = api.dynamics()
    override suspend fun dynamicPage(type: String, offset: String, hostMid: Long?) = api.dynamicPage(type, offset, hostMid)
    override suspend fun dynamicPortal() = api.dynamicPortal()
    override suspend fun dynamicUpList(offset: String) = api.dynamicUpList(offset)
    suspend fun dynamicDetail(id: String) = api.dynamicDetail(id)
    suspend fun likeFeed(id: String, like: Boolean) = api.likeFeed(id, like)
    suspend fun dynamicComments(id: String, page: Int = 1) = api.dynamicComments(id, page)
    suspend fun sendFeedComment(id: String, message: String) = api.sendFeedComment(id, message)
    suspend fun spaceChargeCount(mid: Long) = api.spaceChargeCount(mid)
    suspend fun space(mid: Long) = api.space(mid)
    suspend fun spaceSeasonsSeries(mid: Long) = api.spaceSeasonsSeries(mid)
    suspend fun spaceCollectionArchives( mid: Long, collection: SpaceSeason, ) = api.spaceCollectionArchives(mid, collection)
    suspend fun spaceVideos(mid: Long, page: Int) = api.spaceVideos(mid, page)
    suspend fun spaceArticles(mid: Long, page: Int) = api.spaceArticles(mid, page)
    suspend fun spaceBangumi(mid: Long, page: Int) = api.spaceBangumi(mid, page)
    suspend fun spaceShop(mid: Long) = api.spaceShop(mid)
    suspend fun searchSuggest(term: String) = api.searchSuggest(term)
    suspend fun hotKeywords() = api.hotKeywords()
    suspend fun search( keyword: String, page: Int = 1, /** totalrank 默认 / click 播放多 / pubdate 新发布 / dm 弹幕多 */ order: String = "totalrank", /** 0 不限 / 1 0-10 分钟 / 2 10-30 / 3 30-60 / 4 60 以上 */ duration: Int = 0, pubtimeBegin: Long? = null, pubtimeEnd: Long? = null, ) = api.search(keyword, page, order, duration, pubtimeBegin, pubtimeEnd)
    suspend fun searchUsers(keyword: String, page: Int = 1) = api.searchUsers(keyword, page)
    suspend fun hotSearch() = api.hotSearch()
    suspend fun messages(type: Int, page: Int = 1) = api.messages(type, page)
    suspend fun messagesPage(type: Int, cursor: MessageCursor? = null) = api.messagesPage(type, cursor)
    suspend fun messageSessions( followingOnly: Boolean, sessionTs: Long = 0L, pageSize: Int = 25, onEnriched: ((List<MessageSession>) -> Unit)? = null, ) = api.messageSessions(followingOnly, sessionTs, pageSize, onEnriched)
    suspend fun privateMessages(talkerId: Long, sessionType: Int = 1, size: Int = 30) = api.privateMessages(talkerId, sessionType, size)
    suspend fun privateMessagePage( talkerId: Long, sessionType: Int = 1, size: Int = 30, before: Long? = null, ) = api.privateMessagePage(talkerId, sessionType, size, before)
    suspend fun sendPrivateMessage(talkerId: Long, text: String, sessionType: Int = 1, imageContent: String? = null) = api.sendPrivateMessage(talkerId, text, sessionType, imageContent)
    suspend fun newFollowers(page: Int = 1) = api.newFollowers(page)
    suspend fun userBrief(mid: Long) = api.userBrief(mid)
    suspend fun messageFeedUnread() = api.messageFeedUnread()
    suspend fun messageUnreadCount() = api.messageUnreadCount()
    suspend fun clearAllUnread(sessions: List<MessageSession>) = api.clearAllUnread(sessions)
    suspend fun userIpLocation(mid: Long) = api.userIpLocation(mid)
    suspend fun markSessionsRead(sessions: List<MessageSession>) = api.markSessionsRead(sessions)
    suspend fun removeMessageSession(session: MessageSession) = api.removeMessageSession(session)
    suspend fun pinMessageSession(session: MessageSession, pinned: Boolean) = api.pinMessageSession(session, pinned)
    suspend fun muteMessageSession(session: MessageSession, muted: Boolean) = api.muteMessageSession(session, muted)
    suspend fun deleteFeedMessage(type: Int, id: String) = api.deleteFeedMessage(type, id)
    suspend fun setLikeNotice(id: String, enabled: Boolean) = api.setLikeNotice(id, enabled)
    suspend fun deleteSystemMessage(id: String) = api.deleteSystemMessage(id)
    suspend fun cdnSampleUrl() = api.cdnSampleUrl()
    companion object {
        const val API = BILI_API
        const val APP_API = BILI_APP_API
        const val APP_UA = BILI_APP_UA
        const val APP = BILI_APP
        const val PASSPORT = BILI_PASSPORT
        const val VC = BILI_VC
        const val UA = BILI_UA
        fun image(url: String, width: Int = 0) = biliImage(url, width)
        fun avatar(url: String, size: Int) = biliAvatar(url, size)
        fun cleanHtml(text: String) = cleanBiliHtml(text)
        fun aidToBvid(aid: Long) = biliAidToBvid(aid)
        fun parseCountText(text: String) = parseBiliCountText(text)
        fun parseDurationText(text: String) = parseBiliDurationText(text)
        fun qualityLabel(id: Int) = biliQualityLabel(id)
    }
}
