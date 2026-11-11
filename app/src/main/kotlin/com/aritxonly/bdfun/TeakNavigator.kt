package com.aritxonly.bdfun

import android.content.Context
import android.content.Intent
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.ui.showToast

/** 页面跳转集中在这里：全部走独立 Activity（系统过渡 + 系统返回栈）。 */
object TeakNavigator {
    fun openSettings(context: Context) {
        context.startActivity(Intent(context, SettingsActivity::class.java))
    }

    fun openLibrary(context: Context) {
        context.startActivity(Intent(context, LibraryActivity::class.java))
    }

    fun openHistory(context: Context) {
        context.startActivity(Intent(context, HistoryActivity::class.java))
    }

    fun openWatchLater(context: Context) {
        context.startActivity(Intent(context, WatchLaterActivity::class.java))
    }

    fun openCache(context: Context) {
        context.startActivity(Intent(context, CacheActivity::class.java))
    }

    fun openMessages(context: Context) {
        context.startActivity(Intent(context, MessageActivity::class.java))
    }

    fun openMessageFeed(context: Context, kind: String) {
        context.startActivity(
            Intent(context, MessageDetailActivity::class.java).putExtra("kind", kind),
        )
    }

    fun openPrivateChat(context: Context, session: com.aritxonly.bdfun.data.MessageSession) {
        context.startActivity(
            Intent(context, PrivateChatActivity::class.java)
                .putExtra("talker_id", session.talkerId)
                .putExtra("session_type", session.sessionType)
                .putExtra("name", session.name)
                .putExtra("avatar", session.avatar)
                .putExtra("unread", session.unread)
                .putExtra("max_seqno", session.maxSeqno),
        )
    }

    /** 打开直播详情页。roomId 为 0 时先用 mock 房间。 */
    fun openLiveRoom(context: Context, roomId: Long = 0L) {
        context.startActivity(
            Intent(context, LiveRoomActivity::class.java)
                .putExtra(LiveRoomActivity.EXTRA_ROOM_ID, roomId),
        )
    }

    fun openLogin(context: Context) {
        context.startActivity(Intent(context, LoginActivity::class.java))
    }

    fun openChangelog(context: Context) {
        context.startActivity(Intent(context, ChangelogActivity::class.java))
    }

    /** 从设置里重新看一遍开始页 / 引导。 */
    fun openOnboarding(context: Context) {
        context.startActivity(
            Intent(context, TermsActivity::class.java)
                .putExtra(TermsActivity.EXTRA_REPLAY, true),
        )
    }

    fun openHelp(context: Context) {
        context.startActivity(Intent(context, HelpActivity::class.java))
    }

    fun openDonation(context: Context) {
        context.startActivity(Intent(context, DonationActivity::class.java))
    }

    fun openCredits(context: Context) {
        context.startActivity(Intent(context, CreditsActivity::class.java))
    }

    fun openMySent(context: Context) {
        context.startActivity(Intent(context, MySentActivity::class.java))
    }

    fun openSpace(context: Context, mid: Long) {
        if (mid <= 0) return
        AppGraph.pendingSpaceMid.value = mid
        context.startActivity(Intent(context, SpaceActivity::class.java))
    }

    fun openDynamic(context: Context, dynamic: com.aritxonly.bdfun.data.DynamicItem) {
        AppGraph.pendingDynamic.value = dynamic
        context.startActivity(Intent(context, DynamicDetailActivity::class.java))
    }

    fun openVideo(context: Context, video: VideoItem, fromVideoPage: Boolean = false) {
        AppGraph.pendingVideo.value = video
        context.startActivity(
            Intent(context, VideoActivity::class.java)
                .putExtra("fromVideoPage", fromVideoPage),
        )
    }

    /** 只有 BV 号时直接开播放页，由 VideoActivity 自己拉详情。 */
    fun openVideoByBvid(context: Context, bvid: String) {
        AppGraph.pendingVideo.value = null
        context.startActivity(
            Intent(context, VideoActivity::class.java).putExtra("bvid", bvid),
        )
    }

    /**
     * 从消息页的回复 / 点赞通知回到原视频，并定位到对应评论。
     * 消息接口只给 aid 时，用本地 AV→BV 规则换算，避免再发一次网络请求。
     */
    fun openVideoAtComment(
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
        com.aritxonly.bdfun.ui.VideoPageBus.requestComment(
            root = rootRpid.takeIf { it > 0L } ?: targetRpid,
            target = targetRpid,
            seekMs = seekMs,
        )
        if (resolvedBvid.isNotBlank()) {
            AppGraph.pendingVideo.value = VideoItem(
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
                startPositionMs = seekMs,
            )
        } else {
            // 64 位 av 号：先清掉上一支视频，让 VideoActivity 按 aid 异步换 BV。
            AppGraph.pendingVideo.value = null
        }
        context.startActivity(
            Intent(context, VideoActivity::class.java)
                .putExtra("aid", aid)
                .apply { if (resolvedBvid.isNotBlank()) putExtra("bvid", resolvedBvid) },
        )
    }
}
