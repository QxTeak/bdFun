package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.domain.collectionStartIndex
import com.aritxonly.bdfun.core.domain.videoPlaybackPages
import com.aritxonly.bdfun.ui.FullscreenVideoInfo
import com.aritxonly.bdfun.ui.FullscreenMediaInfo
import com.aritxonly.bdfun.ui.FullscreenInteractionInfo
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.feature.video.application.*

data class VideoPlayerContent(
    val video: VideoItem, val detail: VideoDetail?, val playInfo: PlayInfo?, val quality: Quality?,
    val danmaku: List<DanmakuItem>, val selection: VideoPlaybackSelection,
    val interaction: VideoInteractionState, val metadata: VideoMetadataState, val loggedIn: Boolean,
)
data class VideoPlayerFlags(val fullscreen: Boolean, val immersiveStatusBar: Boolean, val pictureInPicture: Boolean)
data class VideoPlayerAssembly(val media: VideoMediaInput, val presentation: VideoPlayerPresentation)

/** 将应用状态投影为现有播放器入参，不改变封面/标题回退与自动清晰度文案。 */
fun assembleVideoPlayer(content: VideoPlayerContent, flags: VideoPlayerFlags): VideoPlayerAssembly {
    val detail = content.detail
    val video = content.video
    val media = VideoMediaInput(content.selection.bvid, content.playInfo, content.quality, content.selection.autoQuality,
        detail?.cover ?: video.cover, detail?.title ?: video.title, detail?.ownerName ?: video.ownerName, content.danmaku,
        // 多 P 按分 P 数；番剧 / UGC 合集按单集数。
        cacheEpisodeCount = detail?.let { if (it.pages.size > 1) it.pages.size else it.episodes.size } ?: 0)
    return VideoPlayerAssembly(media, VideoPlayerPresentation(
        fullscreenInfo = fullscreenPlayerInfo(content), fullscreen = flags.fullscreen,
        resumePositionMs = content.selection.resumePositionMs, immersiveStatusBar = flags.immersiveStatusBar,
        pictureInPicture = flags.pictureInPicture,
    ))
}

private fun fullscreenPlayerInfo(content: VideoPlayerContent): FullscreenPlayerInfo {
    val detail = content.detail
    val video = content.video
    val state = content.interaction
    val metadata = content.metadata
    return FullscreenPlayerInfo(
        video = FullscreenVideoInfo(
            title = detail?.title ?: video.title,
            author = detail?.ownerName ?: video.ownerName,
            authorFace = detail?.ownerFace ?: video.ownerFace,
            onlineText = metadata.onlineText,
            onlineCount = metadata.onlineCount,
            cover = detail?.cover ?: video.cover,
        ),
        interaction = FullscreenInteractionInfo(
            followed = state.followed,
            liked = state.liked,
            likeCount = state.likeCount,
            replyCount = detail?.reply ?: 0L,
            favorited = state.favorited,
            favoriteCount = state.favoriteCount,
            shareCount = state.shareCount,
            folders = state.folders,
            folderIds = state.folderIds,
            loggedIn = content.loggedIn,
        ),
        media = FullscreenMediaInfo(
            qualityLabel = if (content.selection.autoQuality) "自动" else content.quality?.label ?: "",
            pages = detail?.let(::videoPlaybackPages).orEmpty(),
            partCount = detail?.pages?.size ?: 0,
            collectionStartIndex = detail?.let(::collectionStartIndex) ?: -1,
            currentCid = content.selection.cid,
            subtitles = metadata.subtitles,
            selectedSubtitle = metadata.selectedSubtitle,
            subtitleCues = metadata.subtitleCues,
            danmakuItems = content.danmaku,
            // 评论身份跟详情页评论 tab 同源：PGC 用当前集 aid，UGC 用稿件 aid。
            comments = FullscreenCommentIdentity(
                aid = detail?.aid ?: 0L,
                bvid = detail?.bvid ?: content.selection.bvid,
                upMid = if (detail?.isPgc == true) 0L else detail?.ownerMid ?: 0L,
            ),
        ),
    )
}
