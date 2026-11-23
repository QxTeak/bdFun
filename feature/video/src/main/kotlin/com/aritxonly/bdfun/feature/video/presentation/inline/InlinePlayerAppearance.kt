package com.aritxonly.bdfun.ui

import androidx.compose.ui.graphics.Color

/** app提供原图标资源与强调色，模块不读取业务资源目录。 */
data class InlinePlayerAppearance(
    val playIcon: Int,
    val fullscreenIcon: Int,
    val accentColor: Color,
    /** 与全屏播放控件同一个「下一集」图标；听视频的上一集由它水平镜像得到。 */
    val nextIcon: Int = playIcon,
)
