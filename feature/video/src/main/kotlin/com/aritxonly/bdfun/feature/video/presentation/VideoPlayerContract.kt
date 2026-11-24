package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.video.*

data class VideoMediaInput(
    val bvid: String, val info: PlayInfo?, val quality: Quality?, val autoQuality: Boolean,
    val cover: String, val title: String, val author: String, val danmaku: List<DanmakuItem>,
    /** 可缓存的总集数（多 P / 合集单集）；≤1 时不显示“缓存全部”。 */
    val cacheEpisodeCount: Int = 0,
)
data class VideoPlayerPresentation(
    val fullscreenInfo: FullscreenPlayerInfo, val fullscreen: Boolean,
    val resumePositionMs: Long, val immersiveStatusBar: Boolean, val pictureInPicture: Boolean,
)
class VideoTransportActions(
    val onPlayStateChanged: (Boolean) -> Unit, val onPageSelected: (VideoPage) -> Unit,
    val onSubtitleSelected: (SubtitleTrack?) -> Unit, val onQualitySelected: (Int) -> Unit,
    val onCacheVideo: (Quality) -> Unit,
    /** 选集缓存：一次把当前视频的全部分 P / 合集单集加入队列。 */
    val onCacheAll: (Quality) -> Unit = {},
)
class VideoInteractionActions(
    val onLikeClick: () -> Unit, val onDislikeClick: () -> Unit, val onCommentClick: () -> Unit,
    val onShareClick: () -> Unit, val onOpenSpaceClick: () -> Unit,
    val onCommandFollow: () -> Unit = {}, val onCommandCoin: () -> Unit = {}, val onCommandFavorite: () -> Unit = {},
    val onToggleFolder: (Long) -> Unit, val onRequestFavorites: () -> Unit, val onAddWatchLater: () -> Unit,
)
class VideoNavigationActions(
    val onToggleFullscreen: () -> Unit, val onBackClick: () -> Unit, val onHomeClick: () -> Unit,
)
class VideoPlayerActions(
    val transport: VideoTransportActions, val interaction: VideoInteractionActions, val navigation: VideoNavigationActions,
)
