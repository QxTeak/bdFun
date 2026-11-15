package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize

import com.aritxonly.bdfun.core.domain.DanmakuFilterOptions

internal data class DanmakuLayerDrawing(val opacity: Float, val area: Float, val speed: Float,
    val fontScale: Float, val stroke: Float, val portraitGuard: Boolean, val subtitleGuard: Boolean, val lowRefresh: Boolean,
    /** 顶部给「弹幕计数」让出的高度（dp）；0 表示不挡。 */
    val topGuardDp: Float = 0f)
internal data class DanmakuLayerOptions(val filters: DanmakuFilterOptions, val drawing: DanmakuLayerDrawing)
@Composable
internal fun currentDanmakuLayerOptions(
    fullscreen: Boolean,
    /** 顶部当前是否被「弹幕计数」占着；false 时不预留，弹幕可以从最上面出来。 */
    reserveTopForCounter: Boolean = false,
): DanmakuLayerOptions {
    val settings = com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings.current
    return DanmakuLayerOptions(DanmakuFilterOptions(settings.danmakuDedupe, settings.danmakuBlockScroll,
        settings.danmakuBlockFixed, settings.danmakuBlockColor, settings.danmakuBlockAdvanced,
        settings.danmakuBlockCount, settings.danmakuKeywordFilter, settings.danmakuKeywords),
        DanmakuLayerDrawing(settings.danmakuOpacity, settings.danmakuArea.coerceIn(0.15f, 1f),
            settings.danmakuSpeedPref.coerceAtLeast(0.1f),
            if (fullscreen) settings.danmakuScaleFullscreen else settings.danmakuScaleInline,
            settings.danmakuStroke, false, settings.danmakuSubtitleGuard, settings.danmakuLowRefresh,
            // 计数显示时才让出顶部；引擎只挪空行，已经在屏上的弹幕保持原位。
            topGuardDp = if (reserveTopForCounter) 42f else 0f))
    // 人像防挡在旧入口固定关闭，不恢复读取历史存储值。
}
internal class DanmakuLayerPresentationStateHolder { var viewSize by mutableStateOf(IntSize.Zero) }
