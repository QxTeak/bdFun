package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.video.VideoItemExtras

import com.aritxonly.bdfun.core.model.user.UserProfile
import com.aritxonly.bdfun.core.model.user.SentRecord
import com.aritxonly.bdfun.feature.mine.application.*

internal data class MineScreenRequest(val profile: UserProfile?, val showSentHistory: Boolean, val sessionRevision: Long = 0)
internal class MineDependencies(val createData: (UserProfile?) -> MineStateHolder, val recent: () -> List<SentRecord>,
    val layout: com.aritxonly.bdfun.core.application.MineLayoutStateHolder,
    val createPreview: (UserProfile?) -> MinePreviewStateHolder,
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
