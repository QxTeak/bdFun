package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder
import com.aritxonly.bdfun.feature.video.application.VideoPlaybackSelectionStateHolder

class VideoScreenStateHolders(
    val controllers: VideoDetailControllers,
    val metadata: VideoMetadataStateHolder,
    val ui: VideoDetailUiStateHolder,
    val description: VideoDescriptionStateHolder,
)

@Composable
fun rememberVideoScreenStateHolders(request: VideoScreenRequest, ports: VideoDetailStatePorts): VideoScreenStateHolders {
    val settings = ports.settings
    val readCache = ports.readCache
    val video = request.video
    val detail = remember(video.bvid) { ports.controllers.detail(readCache.related(video.bvid).orEmpty()) }
    val selection = remember(video.bvid) {
        val explicit = when {
            request.initialSeekMs > 0L -> request.initialSeekMs
            settings.resumePlayback -> video.startPositionMs
            else -> 0L
        }
        val local = if (explicit <= 0L && settings.resumePlayback) {
            ports.progress.resumeFor(video.bvid, video.cid)
        } else {
            0L
        }
        VideoPlaybackSelectionStateHolder(video.bvid, video.cid, request.defaultQuality,
            if (explicit > 0L) explicit else local)
    }
    val ui = remember { VideoDetailUiStateHolder() }
    val interaction = remember(video.bvid) { ports.controllers.interaction() }
    val metadata = remember(video.bvid) { ports.controllers.metadata() }
    val description = remember { VideoDescriptionStateHolder() }
    return remember(detail, interaction, selection, metadata, ui, description) {
        VideoScreenStateHolders(VideoDetailControllers(detail, interaction, selection, readCache), metadata, ui, description)
    }
}
