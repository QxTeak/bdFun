package com.aritxonly.bdfun.ui

import android.content.Context
import android.widget.FrameLayout
import com.aritxonly.bdfun.feature.video.presentation.VideoOutput

/**
 * 全屏和半屏复用同一个原生输出宿主，画质变化由平台在宿主内部接力。
 */
internal class PlayerOutputHost(context: Context) : FrameLayout(context) {

    private var output: VideoOutput? = null
    fun bind(create: () -> VideoOutput) {
        if (output != null) return
        val next = create()
        addView(next.view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        output = next
    }

    /** 离开页面时解绑，别让播放器继续攥着一个已经没人看的面。 */
    fun dispose() {
        output?.release()
        output = null
    }
}
