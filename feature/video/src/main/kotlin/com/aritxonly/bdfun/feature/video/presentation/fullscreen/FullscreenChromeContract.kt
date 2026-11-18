package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** 资源归组装壳所有；视频展示只使用传入ID，不反向引用app的R或全局主题。 */
data class FullscreenIconAssets(
    val play: Int, val next: Int, val danmakuOn: Int, val danmakuOff: Int, val danmakuSettings: Int,
    val like: (Boolean) -> Int, val comment: Int, val favorite: Int, val share: Int,
)
data class FullscreenAppearance(
    val accentColor: Color, val icons: FullscreenIconAssets,
    val showActions: Boolean, val showViewerCount: Boolean, val showTime: Boolean,
)

/** 图片请求和跨设置/视频/直播共用的设置内容由app组装，视频布局与点击归本模块。 */
class FullscreenChromeSlots(
    val avatar: @Composable (String, Modifier) -> Unit,
    val cover: @Composable (String, Modifier) -> Unit,
    val danmakuSettings: @Composable (List<com.aritxonly.bdfun.core.model.video.DanmakuItem>) -> Unit,
)
