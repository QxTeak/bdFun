package com.aritxonly.teak.common.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * 全局触感开关。
 *
 * Miuix 组件内部统一走 Compose 的 [androidx.compose.ui.platform.LocalHapticFeedback]，
 * 宿主只需要在 [TeakTheme] 传入设置值；关闭时根节点提供空实现，
 * 组件本身不需要各自判断设置，也不会漏掉某个入口。
 */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/** 设置关闭时替换系统触感实现：所有调用直接丢弃，不触发振动器。 */
internal object SilentHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}
