package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.android.purebilibili.danmaku.engine.DanmakuMaskFrame
import com.android.purebilibili.danmaku.engine.DanmakuRenderConfig
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.android.purebilibili.danmaku.engine.DanmakuWindow
import com.aritxonly.bdfun.core.model.video.DanmakuItem as BiliDanmakuItem


@Composable
internal fun BindDanmakuEngineEffects(renderView: DanmakuRenderView, window: DanmakuWindow,
    masks: List<DanmakuMaskFrame>, config: DanmakuRenderConfig, playing: Boolean, positionMs: () -> Long,
    lowRefresh: Boolean, reservedTopPx: Float, selfItems: List<BiliDanmakuItem> = emptyList()) {
    val engine = renderView.engine
    // 自己发过的弹幕只补一次：window 每次发送都会变，若每次把 selfItems 全量 addImmediate，
    // 之前发过的条目就会被反复补进屏幕（issue #151：发 12、34 后屏幕上出现两个 12）。
    val addedSelfItems = remember(engine) { HashSet<String>() }
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

    LaunchedEffect(window, selfItems) {
        val now = positionMs().coerceAtLeast(0L)
        engine.replaceWindow(window, now)
        if (playing) engine.start(now) else engine.pause()
        // replaceWindow 会按当前进度丢弃“已经发生”的条目，自己刚发的弹幕正好在其中；
        // 这里只把本次新增的即时条目补回去，保证发送后立刻能看到并带圆角底。
        selfItems.forEach { item ->
            if (addedSelfItems.add(item.selfKey())) engine.addImmediate(engineDanmakuItem(item))
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

/** 服务端返回的自发弹幕 id 稳定；没有 id 的本地条目退回 时间+模式+文本。 */
private fun BiliDanmakuItem.selfKey(): String =
    if (id > 0L) "id:$id" else "t:$timeMs:$mode:$text"
