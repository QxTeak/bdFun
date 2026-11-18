package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class VideoDetailPlayerActionBindings(
    private val scope: CoroutineScope, private val detail: VideoDetail?,
    private val ui: VideoDetailUiStateHolder, private val actions: VideoDetailViewActions,
    private val metadataController: VideoMetadataStateHolder, private val events: VideoScreenEvents,
) {
    private var playing by ui.playing
    private var fullscreen by ui.fullscreen
    fun create(): VideoPlayerActions = VideoPlayerActions(
    transport = VideoTransportActions(
        onPlayStateChanged = { playing = it },
        onPageSelected = { actions.switchPage(it) },
        onSubtitleSelected = { track -> scope.launch { metadataController.selectSubtitle(track) } },
        onQualitySelected = { actions.switchQuality(it) },
        onCacheVideo = { actions.cacheVideo(it) },
    ),
    interaction = VideoInteractionActions(
        onLikeClick = { actions.toggleLike() },
        onDislikeClick = { actions.toggleDislike() },
        onCommentClick = {
            val d = detail
            if (d != null) {
                fullscreen = false
                events.onOpenComments(d)
            }
        },
        onShareClick = { actions.shareVideo() },
        onOpenSpaceClick = { actions.toggleFollow() },
        onCommandFollow = { actions.followFromDanmaku() },
        onCommandCoin = { actions.giveCoin() },
        onCommandFavorite = { actions.toggleFavorite() },
        onToggleFolder = { actions.toggleFolder(it) },
        onRequestFavorites = { actions.ensureFavoritesLoaded() },
        onAddWatchLater = { actions.addWatchLater() },
    ),
    navigation = VideoNavigationActions(
        onToggleFullscreen = { fullscreen = !fullscreen },
        onBackClick = {
            when {
                fullscreen -> fullscreen = false
                else -> actions.leaveVideo()
            }
        },
        onHomeClick = { actions.leaveVideo() },
    ),
)
}
