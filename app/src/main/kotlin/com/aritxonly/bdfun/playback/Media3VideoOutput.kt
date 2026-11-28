@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.TextureView
import android.widget.FrameLayout
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aritxonly.bdfun.R
import com.aritxonly.bdfun.core.application.PlaybackVideoOutputMode
import com.aritxonly.bdfun.feature.video.presentation.VideoOutput
import com.aritxonly.bdfun.infra.playback.Media3PlaybackController
import com.aritxonly.bdfun.infra.playback.Media3VideoHandoffOutput

internal class Media3VideoOutput(
    context: Context,
    private val controller: Media3PlaybackController,
) : VideoOutput, Media3VideoHandoffOutput {
    override val view = FrameLayout(context)
    private var visible: PlayerView? = null
    private var staged: PlayerView? = null
    private var visibleMode = PlaybackVideoOutputMode.Standard
    private var stagedMode = PlaybackVideoOutputMode.Standard

    override var resizeMode: Int = 0
        set(value) {
            field = value
            visible?.resizeMode = aspectMode()
            staged?.resizeMode = aspectMode()
        }

    init {
        controller.attachVideoOutput(this)
    }

    override fun show(player: Player, mode: PlaybackVideoOutputMode) {
        clearStaged()
        if (visible?.player === player && visibleMode == mode) return
        remove(visible)
        visibleMode = mode
        visible = newView(player, mode).also { view.addView(it, size()) }
    }

    override fun stage(player: Player, mode: PlaybackVideoOutputMode) {
        clearStaged()
        stagedMode = mode
        // 保持新表面可解码，但放在旧视频视图后面。接力时不搬动已出帧的新表面。
        staged = newView(player, mode).also { view.addView(it, 0, size()) }
    }

    override fun promote() {
        val next = staged ?: return
        remove(visible)
        visible = next
        visibleMode = stagedMode
        staged = null
    }

    override fun clearStaged() {
        remove(staged)
        staged = null
    }

    override fun captureThirdSize(): Bitmap? {
        val texture = visible?.videoSurfaceView as? TextureView ?: return null
        if (texture.width <= 0 || texture.height <= 0) return null
        val width = (texture.width / 3).coerceAtLeast(1)
        val height = (texture.height / 3).coerceAtLeast(1)
        return runCatching { texture.getBitmap(width, height) }.getOrNull()
    }

    override fun release() {
        controller.detachVideoOutput(this)
        clearStaged()
        remove(visible)
        visible = null
    }

    private fun newView(player: Player, mode: PlaybackVideoOutputMode): PlayerView {
        // 杜比视界通过 SurfaceView 直通，普通画质保留纹理变换和抓帧。
        val layout = if (mode == PlaybackVideoOutputMode.DolbyVision) {
            R.layout.teak_player_view_surface
        } else R.layout.teak_player_view
        return (LayoutInflater.from(view.context).inflate(layout, view, false) as PlayerView).apply {
            keepScreenOn = false
            setKeepContentOnPlayerReset(true)
            useController = false
            resizeMode = aspectMode()
            setShutterBackgroundColor(Color.BLACK)
            if (mode != PlaybackVideoOutputMode.DolbyVision) setBackgroundColor(Color.BLACK)
            this.player = player
        }
    }

    private fun remove(playerView: PlayerView?) {
        if (playerView == null) return
        playerView.player = null
        view.removeView(playerView)
    }

    private fun size() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
    )

    private fun aspectMode() = when (resizeMode) {
        1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }
}
