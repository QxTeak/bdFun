package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.video.VideoItemExtras

import com.aritxonly.bdfun.core.model.user.UserProfile
import com.aritxonly.bdfun.core.model.user.SentRecord
import com.aritxonly.bdfun.feature.mine.application.*

internal data class MineScreenRequest(
    val profile: UserProfile?, val showSentHistory: Boolean, val sessionRevision: Long = 0,
    /** 登录态资料仍在刷新：我的页显示骨架屏，避免先闪游客卡再切换。 */
    val sessionLoading: Boolean = false,
    /** 已登录（cookie 还在）但资料未回来；游客不能显示骨架屏。 */
    val loggedIn: Boolean = false,
)
internal class MineDependencies(val createData: (UserProfile?) -> MineStateHolder, val recent: () -> List<SentRecord>,
    val layout: com.aritxonly.bdfun.core.application.MineLayoutStateHolder,
    val createPreview: (UserProfile?) -> MinePreviewStateHolder,
    /** 预览卡里的视频也要先进播放队列，否则播完没有下一集可连。 */
    val playbackQueue: com.aritxonly.bdfun.core.application.PlaybackQueueStateHolder,
)
internal class MineNavigation(
    val onLogin: () -> Unit, val onOpenSettings: () -> Unit, val onOpenLibrary: () -> Unit,
    val onOpenHistory: () -> Unit, val onOpenWatchLater: () -> Unit, val onOpenCache: () -> Unit,
    val onOpenSentHistory: () -> Unit, val onOpenSpace: (Long) -> Unit,
    val onOpenRelation: (Long, Boolean) -> Unit, val onOpenVideo: (com.aritxonly.bdfun.core.model.video.VideoItem) -> Unit,
) {
    fun openRecord(record: SentRecord) {
        if (record.bvid.isNotBlank()) onOpenVideo(com.aritxonly.bdfun.core.model.video.VideoItem(
            bvid = record.bvid,
            aid = 0,
            title = record.title,
            cover = "",
            ownerName = "",
            ownerFace = "",
            ownerMid = 0,
            play = 0,
            danmaku = 0,
            duration = 0,
            pubdate = 0,
            extras = VideoItemExtras(
                startPositionMs = record.videoTimeMs,
            ),
        ))
    }
}
