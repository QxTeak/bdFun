package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aritxonly.bdfun.core.model.video.DanmakuItem

/**
 * 保留原弹幕适配入口，视频模块不跨层读取app设置或构造弹幕引擎。
 *
 * [danmaku] 的最后一个参数表示"当前顶部是否要让给弹幕计数"：
 * 只有计数真的显示时才为 true，弹幕引擎据此只把空行往下挪，已经在屏上的弹幕不动。
 */
class PlayerSurfaceSlots(
    val danmaku: @Composable (
        List<DanmakuItem>, () -> Long, Boolean, Float, Modifier, Boolean, Boolean,
    ) -> Unit,
)
data class VideoPictureSettings(
    val progressBarMode: Int,
    val hotDanmakuEnabled: Boolean,
    val accentColor: Color,
    /** 弹幕计数沿用普通弹幕的描边宽度与字号缩放，保证观感一致。 */
    val hotDanmakuStrokeDp: Float = 1.5f,
    val hotDanmakuTextScale: Float = 1f,
)
