package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class PlayerGestureOptions(
    val verticalGesturesEnabled: () -> Boolean,
    val boostDefaultSpeed: () -> Float,
    val centerSwipeEnabled: () -> Boolean,
    val boostSwipeEnabled: () -> Boolean,
    /** 全局震动开关：关闭后长按加速等手势不再触发触感。 */
    val hapticsEnabled: () -> Boolean = { true },
)
class PlayerFullscreenVisuals(
    val appearance: FullscreenAppearance, val slots: FullscreenChromeSlots,
    val danmakuOn: Boolean, val backgroundPlay: Boolean,
)

typealias StyledDanmakuSender = suspend (String, Int, Int, Int, com.aritxonly.bdfun.core.domain.DanmakuSendAppearance) -> Boolean

class PlayerDanmakuComposePorts(
    val sender: () -> StyledDanmakuSender?,
    val allowVipColor: Boolean,
    val allowUpIdentity: Boolean,
)

/** app提供原平台动作/资源/共用弹幕适配；模块拥有原绘制与指针树。 */
class PlayerRenderPorts(
    val page: PlayerPageHost,
    val gestures: PlayerGestureOptions,
    val fullscreenActions: FullscreenActions,
    val picture: VideoPictureSettings,
    val surface: PlayerSurfaceSlots,
    val buffering: @Composable (Modifier) -> Unit,
    val cover: @Composable (String, Modifier) -> Unit,
    val inlineAppearance: () -> InlinePlayerAppearance,
    val fullscreenVisuals: @Composable () -> PlayerFullscreenVisuals,
    val danmakuComposer: PlayerDanmakuComposePorts,
    val danmakuOverlays: @Composable androidx.compose.foundation.layout.BoxScope.(Boolean) -> Unit,
    val inlineMore: @Composable () -> Unit,
    /** 播放/暂停按钮那颗三角（drawable 资源）；长按倍速提示复用它，不再自绘。 */
    val playIcon: Int,
)
