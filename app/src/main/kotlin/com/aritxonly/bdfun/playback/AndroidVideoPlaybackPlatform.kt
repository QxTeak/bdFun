@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.playback

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.C
import com.aritxonly.bdfun.core.application.MediaControlsRegistry
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.feature.video.presentation.PlaybackAudioEffect
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform
import com.aritxonly.bdfun.infra.bili.BiliBackend
import com.aritxonly.bdfun.infra.playback.Media3PlaybackController
import com.aritxonly.bdfun.infra.playback.MediaPlaybackSession
import com.aritxonly.bdfun.ui.MediaActionReceiver

/** 只在 app 装配平台实现；传输命令仍落到原来的单个控制器。 */
internal class AndroidVideoPlaybackPlatform(
    private val controller: Media3PlaybackController,
    private val registry: MediaControlsRegistry,
) : VideoPlaybackPlatform, PlaybackController by controller {
    override val userAgent = BiliBackend.UA
    override fun createOutput(context: Context) = Media3VideoOutput(context, controller.player)
    override fun setVideoEnabled(enabled: Boolean) {
        val exo = controller.player
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !enabled)
            .build()
    }
    override fun createLoudnessEffect(): PlaybackAudioEffect? {
        val exo = controller.player
        if (exo.audioSessionId <= 0) return null
        val effect = runCatching {
            LoudnessEnhancer(exo.audioSessionId).apply {
                setTargetGain(600)
                enabled = true
            }
        }.getOrNull() ?: return null
        return PlaybackAudioEffect { effect.release() }
    }
    override fun createMediaSession(
        context: Context, title: String, artist: String, artworkUrl: String?,
        onQueueSelected: (Int) -> Unit,
    ) = MediaPlaybackSession(context, registry, MediaActionReceiver::class.java, this,
        title, artist, artworkUrl, onQueueSelected)
}
