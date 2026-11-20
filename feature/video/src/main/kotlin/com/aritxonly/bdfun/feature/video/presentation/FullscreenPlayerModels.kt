package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.library.FavoriteFolder
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import com.aritxonly.bdfun.core.model.video.Quality
import com.aritxonly.bdfun.core.model.video.SubtitleCue
import com.aritxonly.bdfun.core.model.video.SubtitleTrack
import com.aritxonly.bdfun.core.model.video.VideoPage

enum class FullscreenPanel {
    None,
    Favorite,
    More,
    Danmaku,
    Episodes,
    Speed,
    Quality,
    Subtitles,
    Cache,
}

val FullscreenSpeeds = listOf(2f, 1.5f, 1.25f, 1f, 0.75f, 0.5f)

data class FullscreenPlayerInfo(
    val video: FullscreenVideoInfo,
    val interaction: FullscreenInteractionInfo,
    val media: FullscreenMediaInfo,
) {
    val title get() = video.title
    val author get() = video.author
    val authorFace get() = video.authorFace
    val onlineText get() = video.onlineText
    val onlineCount get() = video.onlineCount
    val followed get() = interaction.followed
    val liked get() = interaction.liked
    val likeCount get() = interaction.likeCount
    val replyCount get() = interaction.replyCount
    val favorited get() = interaction.favorited
    val favoriteCount get() = interaction.favoriteCount
    val shareCount get() = interaction.shareCount
    val qualityLabel get() = media.qualityLabel
    val cover get() = video.cover
    val pages get() = media.pages
    val currentCid get() = media.currentCid
    val folders get() = interaction.folders
    val folderIds get() = interaction.folderIds
    val loggedIn get() = interaction.loggedIn
    val subtitles get() = media.subtitles
    val selectedSubtitle get() = media.selectedSubtitle
    val subtitleCues get() = media.subtitleCues
    val danmakuItems get() = media.danmakuItems
    val videoAspectRatio get() = media.videoAspectRatio
}

data class FullscreenVideoInfo(
    val title: String,
    val author: String,
    val authorFace: String,
    val onlineText: String,
    val onlineCount: Long,
    val cover: String,
)

data class FullscreenInteractionInfo(
    val followed: Boolean = false,
    val liked: Boolean,
    val likeCount: Long,
    val replyCount: Long,
    val favorited: Boolean,
    val favoriteCount: Long,
    val shareCount: Long,
    val folders: List<FavoriteFolder>,
    val folderIds: Set<Long>,
    val loggedIn: Boolean,
)

data class FullscreenMediaInfo(
    val qualityLabel: String,
    val pages: List<VideoPage>,
    val currentCid: Long,
    val subtitles: List<SubtitleTrack>,
    val selectedSubtitle: SubtitleTrack?,
    val subtitleCues: List<SubtitleCue>,
    val danmakuItems: List<DanmakuItem> = emptyList(),
    // 视频宽高比（宽/高）；接口没给宽高时为空，按横屏处理。
    val videoAspectRatio: Float? = null,
)


data class FullscreenPlaybackState(
    val transport: FullscreenTransport,
    val options: FullscreenPlaybackOptions,
) {
    val playing get() = transport.playing
    val positionMs get() = transport.positionMs
    val durationMs get() = transport.durationMs
    val bufferedMs get() = transport.bufferedMs
    val speed get() = transport.speed
    val qualities get() = options.qualities
    val qualityId get() = options.qualityId
    val autoQuality get() = options.autoQuality
    val danmakuEnabled get() = options.danmakuEnabled
    val autoPlayNext get() = options.autoPlayNext
    val loopMode get() = options.loopMode
    val mirror get() = options.mirror
    val resizeMode get() = options.resizeMode
    val colorAssist get() = options.colorAssist
    val audioOnly get() = options.audioOnly
    val sleepAtMs get() = options.sleepAtMs
    val backgroundPlay get() = options.backgroundPlay
    val seekLook get() = transport.seekLook
    val scrubbing get() = transport.scrubbing
}

data class FullscreenTransport(
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val bufferedMs: Long = 0L,
    val speed: Float,
    val seekLook: Float = 0f,
    val scrubbing: Boolean = false,
)

data class FullscreenPlaybackOptions(
    val qualities: List<Quality>,
    val qualityId: Int,
    val autoQuality: Boolean = false,
    val danmakuEnabled: Boolean,
    val autoPlayNext: Boolean,
    val loopMode: Int,
    val mirror: Boolean,
    val resizeMode: Int,
    val colorAssist: Int,
    val audioOnly: Boolean,
    val sleepAtMs: Long,
    val backgroundPlay: Boolean,
)


class FullscreenActions(
    val navigation: FullscreenNavigationActions,
    val playback: FullscreenTransportActions,
    val social: FullscreenSocialActions,
    val preferences: FullscreenPreferenceActions,
) {
    val onBack get() = navigation.onBack
    val onPlayPause get() = playback.onPlayPause
    val onNext get() = navigation.onNext
    val onEpisodes get() = navigation.onEpisodes
    val onLike get() = social.onLike
    val onComment get() = navigation.onComment
    val onShare get() = social.onShare
    val onOpenSpace get() = navigation.onOpenSpace
    val onToggleFolder get() = social.onToggleFolder
    val onRequestFavorites get() = social.onRequestFavorites
    val onSeek get() = playback.onSeek
    val onQualitySelected get() = playback.onQualitySelected
    val onOpenCachePanel get() = navigation.onOpenCachePanel
    val onCacheSelected get() = playback.onCacheSelected
    val onSpeedSelected get() = playback.onSpeedSelected
    val onPageSelected get() = navigation.onPageSelected
    val onSubtitleSelected get() = playback.onSubtitleSelected
    val onSendDanmaku get() = social.onSendDanmaku
    val onToggleDanmaku get() = playback.onToggleDanmaku
    val onAddWatchLater get() = social.onAddWatchLater
    val onPictureInPicture get() = navigation.onPictureInPicture
    val onCast get() = navigation.onCast
    val onAudioOnlyChanged get() = preferences.onAudioOnlyChanged
    val onAutoPlayNextChanged get() = preferences.onAutoPlayNextChanged
    val onLoopModeChanged get() = preferences.onLoopModeChanged
    val onMirrorChanged get() = preferences.onMirrorChanged
    val onResizeModeChanged get() = preferences.onResizeModeChanged
    val onColorAssistChanged get() = preferences.onColorAssistChanged
    val onSleepAtChanged get() = preferences.onSleepAtChanged
    val onBackgroundPlayChanged get() = preferences.onBackgroundPlayChanged
    val onSeekRelative get() = playback.onSeekRelative
}

data class FullscreenNavigationActions(
    val onBack: () -> Unit,
    val onNext: () -> Unit,
    val onEpisodes: () -> Unit,
    val onComment: () -> Unit,
    val onOpenSpace: () -> Unit,
    val onOpenCachePanel: () -> Unit,
    val onPageSelected: (VideoPage) -> Unit,
    val onPictureInPicture: () -> Unit,
    val onCast: () -> Unit,
)

data class FullscreenSocialActions(
    val onLike: () -> Unit,
    val onShare: () -> Unit,
    val onToggleFolder: (Long) -> Unit,
    val onRequestFavorites: () -> Unit,
    val onSendDanmaku: () -> Unit,
    val onAddWatchLater: () -> Unit,
)

data class FullscreenTransportActions(
    val onPlayPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onQualitySelected: (Int) -> Unit,
    val onCacheSelected: (Quality) -> Unit,
    val onSpeedSelected: (Float) -> Unit,
    val onSubtitleSelected: (SubtitleTrack?) -> Unit,
    val onToggleDanmaku: () -> Unit,
    val onSeekRelative: (Long) -> Unit,
)

data class FullscreenPreferenceActions(
    val onAudioOnlyChanged: (Boolean) -> Unit,
    val onAutoPlayNextChanged: (Boolean) -> Unit,
    val onLoopModeChanged: (Int) -> Unit,
    val onMirrorChanged: (Boolean) -> Unit,
    val onResizeModeChanged: (Int) -> Unit,
    val onColorAssistChanged: (Int) -> Unit,
    val onSleepAtChanged: (Long) -> Unit,
    val onBackgroundPlayChanged: (Boolean) -> Unit,
)
