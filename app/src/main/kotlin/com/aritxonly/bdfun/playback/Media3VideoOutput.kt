@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.playback

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.TextureView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aritxonly.bdfun.R
import com.aritxonly.bdfun.feature.video.presentation.VideoOutput

/** 使用原 XML 与同一个 PlayerView，不给展示树添加包装节点。 */
internal class Media3VideoOutput(context: Context, player: Player) : VideoOutput {
    override val view = (LayoutInflater.from(context).inflate(R.layout.teak_player_view, null) as PlayerView).apply {
        this.player = player
        useController = false
        setShutterBackgroundColor(android.graphics.Color.BLACK)
    }
    override var resizeMode: Int
        get() = when (view.resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> 1
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> 2
            else -> 0
        }
        set(value) {
            view.resizeMode = when (value) {
                1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
    override fun captureThirdSize(): Bitmap? {
        val tv = view.videoSurfaceView as? TextureView ?: return null
        if (tv.width <= 0 || tv.height <= 0) return null
        val w = (tv.width / 3).coerceAtLeast(1)
        val h = (tv.height / 3).coerceAtLeast(1)
        return runCatching { tv.getBitmap(w, h) }.getOrNull()
    }
}
