package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.android.purebilibili.danmaku.engine.DanmakuMaskFrame
import com.android.purebilibili.danmaku.engine.DanmakuRenderConfig
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.android.purebilibili.danmaku.engine.DanmakuWindow


@Composable
internal fun BindDanmakuEngineEffects(renderView: DanmakuRenderView, window: DanmakuWindow,
    masks: List<DanmakuMaskFrame>, config: DanmakuRenderConfig, playing: Boolean, positionMs: () -> Long,
    lowRefresh: Boolean, reservedTopPx: Float) {
    val engine = renderView.engine
    // 降低弹幕刷新率：默认开启（60 帧），关闭后交给屏幕刷新率。
    LaunchedEffect(lowRefresh) {
        renderView.setMaxFrameRate(if (lowRefresh) 60 else 0)
    }

    // 顶部让位只影响后续新弹幕，不触发重新排版，也不移动屏上已有弹幕。
    LaunchedEffect(reservedTopPx) {
        engine.setReservedTop(reservedTopPx)
    }

    LaunchedEffect(config) {
        engine.updateConfig(config)
    }

    LaunchedEffect(window) {
        engine.replaceWindow(window, positionMs().coerceAtLeast(0L))
        if (playing) {
            engine.start(positionMs().coerceAtLeast(0L))
        } else {
            engine.pause()
        }
    }

    LaunchedEffect(masks) {
        engine.replaceMaskFrames(masks, positionMs().coerceAtLeast(0L))
    }

    LaunchedEffect(playing) {
        if (playing) {
            engine.start(positionMs().coerceAtLeast(0L))
        } else {
            engine.pause()
        }
    }

    BindDanmakuEngineClock(engine, playing, positionMs)
}
