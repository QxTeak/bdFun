package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aritxonly.bdfun.core.model.video.DanmakuItem

/**
 * 保留原弹幕适配入口，视频模块不跨层读取app设置或构造弹幕引擎。
 *
 * 高赞栏提供实际占用高度和已展示的条目，渲染层据此让位与去重。
 */
class PlayerSurfaceSlots(
    val danmaku: @Composable (
        List<DanmakuItem>, () -> Long, Boolean, Float, Modifier, Boolean, HotDanmakuPresentation, PlayerDanmakuHit,
    ) -> Unit,
)
data class VideoPictureSettings(
    val progressBarMode: Int,
    val hotDanmakuEnabled: Boolean,
    val accentColor: Color,
    /** 弹幕计数沿用普通弹幕的描边宽度与字号缩放，保证观感一致。 */
    val hotDanmakuStrokeDp: Float = 1.5f,
    val hotDanmakuTextScale: Float = 1f,
    val danmakuOpacity: Float = 1f,
)
