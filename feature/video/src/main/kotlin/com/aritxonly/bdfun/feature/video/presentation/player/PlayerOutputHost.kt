package com.aritxonly.bdfun.ui

import android.content.Context
import android.widget.FrameLayout
import com.aritxonly.bdfun.feature.video.presentation.VideoOutput

/**
 * 播放器输出表面的宿主。
 *
 * 绝大部分画质走纹理表面（抓帧、双指缩放/旋转、镜像、色觉辅助都依赖 GPU 合成）；
 * 只有杜比视界必须换成直通表面，否则 DV 元数据在合成时丢失，整帧变绿紫。
 * 换表面时先解绑旧视图再挂新视图，避免旧 PlayerView 解绑时把新绑定的表面清掉。
 */
internal class PlayerOutputHost(context: Context) : FrameLayout(context) {

    private var output: VideoOutput? = null
    private var direct = false

    fun bind(forceDirect: Boolean, create: (Boolean) -> VideoOutput) {
        if (output != null && direct == forceDirect) return
        val previous = output
        previous?.release()
        val next = create(forceDirect)
        addView(next.view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        previous?.let { removeView(it.view) }
        output = next
        direct = forceDirect
    }

    /** 离开页面时解绑，别让播放器继续攥着一个已经没人看的面。 */
    fun dispose() {
        output?.release()
        output = null
    }
}
