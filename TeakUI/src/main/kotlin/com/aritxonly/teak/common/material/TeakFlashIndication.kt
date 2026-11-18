package com.aritxonly.teak.common.material

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixIndication

/**
 * 程序触发的「闪一下灰」。
 *
 * 视觉完全复用设置页 Preference 被按下时的 MiuixIndication（同一份灰底与同一组弹簧），
 * 只是按压由代码合成：跳转定位、发送成功等没有手指按下的场景也能看到和手动点击一致的反馈，
 * 不再另写一套快速淡入淡出的高亮动画。
 *
 * [flashKey] 为 null 表示不高亮；同一个目标重复触发时传入不同的值即可重新闪一次。
 */
@Composable
fun Modifier.teakFlashIndication(
    flashKey: Any?,
    shape: Shape = RectangleShape,
    holdMillis: Long = 160L,
): Modifier {
    if (flashKey == null) return this
    val color = MiuixTheme.colorScheme.onBackground
    val source = remember { MutableInteractionSource() }
    val indication = remember(color) { MiuixIndication(color) }
    LaunchedEffect(flashKey, holdMillis) {
        val press = PressInteraction.Press(Offset.Unspecified)
        source.emit(press)
        try {
            delay(holdMillis)
        } finally {
            // 组合被销毁时也要解除按压，否则灰底会停在被按下的状态。
            source.tryEmit(PressInteraction.Release(press))
        }
    }
    return clip(shape).indication(source, indication)
}
