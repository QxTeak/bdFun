package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigation.createRouteIntent
import com.aritxonly.bdfun.core.model.video.VideoItemExtras
import android.content.Context
import android.content.Intent
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.ui.showToast

/** 页面跳转集中在这里：全部走独立 Activity（系统过渡 + 系统返回栈）。 */
fun navigateToSettings(context: Context) {
    context.startActivity(Intent(context, SettingsActivity::class.java))
}

fun navigateToLibrary(context: Context) {
    context.startActivity(Intent(context, LibraryActivity::class.java))
}

fun navigateToHistory(context: Context) {
    context.startActivity(Intent(context, HistoryActivity::class.java))
}

fun navigateToWatchLater(context: Context) {
    context.startActivity(Intent(context, WatchLaterActivity::class.java))
}

fun navigateToCache(context: Context) {
    context.startActivity(Intent(context, CacheActivity::class.java))
}

fun navigateToMessages(context: Context) {
    context.startActivity(Intent(context, MessageActivity::class.java))
}

fun navigateToMessageFeed(context: Context, kind: String) {
    context.startActivity(
        Intent(context, MessageDetailActivity::class.java).putExtra("kind", kind),
    )
}

fun navigateToPrivateChat(context: Context, session: com.aritxonly.bdfun.data.MessageSession) {
    context.startActivity(
        createRouteIntent(context,
            com.aritxonly.bdfun.core.navigation.Route.PrivateChat(session.talkerId, session.sessionType,
                session.name, session.avatar, session.unread, session.maxSeqno)),
    )
}

fun navigateToLogin(context: Context) {
    context.startActivity(Intent(context, LoginActivity::class.java))
}

fun navigateToChangelog(context: Context) {
    context.startActivity(Intent(context, ChangelogActivity::class.java))
}

/** 从设置里重新看一遍开始页 / 引导。 */
fun navigateToOnboarding(context: Context) {
    context.startActivity(
        Intent(context, TermsActivity::class.java)
            .putExtra(TermsActivity.EXTRA_REPLAY, true),
    )
}

fun navigateToHelp(context: Context) {
    context.startActivity(Intent(context, HelpActivity::class.java))
}

fun navigateToDonation(context: Context) {
    context.startActivity(Intent(context, DonationActivity::class.java))
}

fun navigateToCredits(context: Context) {
    context.startActivity(Intent(context, CreditsActivity::class.java))
}

fun navigateToMySent(context: Context) {
    context.startActivity(Intent(context, MySentActivity::class.java))
}

fun navigateToSpace(context: Context, mid: Long) {
    if (mid <= 0) return
    context.startActivity(createRouteIntent(context, Route.Space(mid)))
}

fun navigateToFeed(context: Context, dynamic: com.aritxonly.bdfun.data.FeedItem) {
    context.startActivity(createRouteIntent(context, Route.Feed(dynamic.id, dynamic)))
}

fun navigateToVideo(context: Context, video: VideoItem, fromVideoPage: Boolean = false) {
    context.startActivity(createRouteIntent(context,
        Route.Video(video.bvid, video.aid, video, fromVideoPage)))
}

/** 只有 BV 号时直接开播放页，由 VideoActivity 自己拉详情。 */
fun navigateToVideoByBvid(context: Context, bvid: String) {
    context.startActivity(createRouteIntent(context, Route.Video(bvid = bvid)))
}

/**
 * 从消息页的回复 / 点赞通知回到原视频，并定位到对应评论。
 * 消息接口只给 aid 时，用本地 AV→BV 规则换算，避免再发一次网络请求。
 */
fun navigateToVideoAtComment(
    context: Context,
    aid: Long,
    bvid: String,
    title: String,
    rootRpid: Long,
    targetRpid: Long,
    seekMs: Long = 0L,
) {
    // 只用旧公式换算 32 位 av 号；64 位新 av 号（> Int.MAX_VALUE）必须让接口换 BV。
    val resolvedBvid = when {
        bvid.isNotBlank() -> bvid
        aid in 1L..Int.MAX_VALUE.toLong() ->
            com.aritxonly.bdfun.data.BiliApi.aidToBvid(aid)
        else -> ""
    }
    com.aritxonly.bdfun.data.LogCollector.record(
        "MSGVIDEO",
        "aid=$aid inBvid=$bvid resolved=$resolvedBvid root=$rootRpid target=$targetRpid",
    )
    if (aid <= 0L && resolvedBvid.isBlank()) {
        context.showToast("这条消息没有可打开的视频")
        return
    }
    val snapshot = if (resolvedBvid.isNotBlank()) VideoItem(
        bvid = resolvedBvid,
        aid = aid,
        title = title,
        cover = "",
        ownerName = "",
        ownerFace = "",
        ownerMid = 0L,
        play = 0L,
        danmaku = 0L,
        duration = 0,
        pubdate = 0L,
        extras = VideoItemExtras(
            startPositionMs = seekMs,
        ),
    ) else null
    context.startActivity(createRouteIntent(context, Route.Video(
        resolvedBvid, aid, snapshot,
        commentRoot = rootRpid.takeIf { it > 0L } ?: targetRpid,
        commentTarget = targetRpid, seekMs = seekMs,
    )))
}
/** roomId为0时沿用main的热门房间/本地占位回退。 */
fun navigateToLiveRoom(context: Context, roomId: Long = 0L) {
    context.startActivity(Intent(context, LiveRoomActivity::class.java)
        .putExtra(LiveRoomActivity.EXTRA_ROOM_ID, roomId))
}
