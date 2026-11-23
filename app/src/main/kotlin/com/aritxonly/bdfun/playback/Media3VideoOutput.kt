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

/**
 * 使用原 XML 与同一个 PlayerView，不给展示树添加包装节点。
 *
 * [direct] = true 时换成 SurfaceView 直通表面：杜比视界的元数据只有走硬件叠层才能到屏幕，
 * 其余画质仍用纹理表面（缩放/镜像/色觉辅助/抓帧依赖它）。
 */
internal class Media3VideoOutput(
    context: Context,
    player: Player,
    direct: Boolean = false,
) : VideoOutput {
    private val layout = if (direct) R.layout.teak_player_view_surface else R.layout.teak_player_view
    override val view = (LayoutInflater.from(context).inflate(layout, null) as PlayerView).apply {
        // 换 CDN/清晰度会重新 prepare，保留上一帧直到新首帧到达。
        setKeepContentOnPlayerReset(true)
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
    override fun release() {
        // 先解绑再挂新输出：否则旧 PlayerView 解绑时可能把新表面一起清掉。
        view.player = null
    }
}
