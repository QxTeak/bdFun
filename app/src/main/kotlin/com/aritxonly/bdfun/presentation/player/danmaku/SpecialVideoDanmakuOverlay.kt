package com.aritxonly.bdfun.ui

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import com.android.purebilibili.danmaku.parser.bas.BasTarget
import com.android.purebilibili.danmaku.parser.TimedDanmakuWindow
import com.android.purebilibili.danmaku.parser.DanmakuParser
import com.android.purebilibili.danmaku.parser.DanmakuProto
import com.android.purebilibili.danmaku.parser.AdvancedDanmakuData
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.domain.DanmakuRuleMatcher
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

@Composable
internal fun SpecialVideoDanmakuOverlay(
    items: List<DanmakuItem>, playback: PlaybackController, ui: VideoPlayerUiStateHolder, aid: Long, cid: Long,
) {
    val page = LocalVideoPageState.current
    val controller = page.danmakuModeration ?: return
    val state by controller.state.collectAsState()
    var remote by remember(aid, cid) { mutableStateOf(emptyList<DanmakuItem>()) }
    var program by remember(aid, cid) { mutableStateOf(SpecialDanmakuProgram()) }
    LaunchedEffect(controller, aid, cid, playback) {
        val source = controller.specialDanmaku(aid, cid) ?: return@LaunchedEffect
        playback.state.map { (it.positionMs / 2_000L) * 2_000L }.distinctUntilChanged().collect { start ->
            remote = source.readWindow(start)
        }
    }
    LaunchedEffect(items, remote, state.rules, state.recalled) {
        program = withContext(Dispatchers.Default) {
            val rules = DanmakuRuleMatcher(state.rules.filter { it.kind == com.aritxonly.bdfun.core.domain.DanmakuRuleKind.Sender })
            val advanced = mutableListOf<AdvancedDanmakuData>()
            val bas = mutableListOf<BasScenePainter>()
            (items.asSequence().filter { it.mode == 7 || it.mode == 9 } + remote.asSequence())
                .distinctBy { Triple(it.id, it.timeMs, it.text) }
                .filterNot { rules.blocks(it) || (cid to it.id) in state.recalled }
                .forEach { item ->
                    ensureActive()
                    val parsed = runCatching {
                        DanmakuParser.parseElement(DanmakuProto.DanmakuElem(
                            id = item.id, progress = item.timeMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                            mode = item.mode, fontsize = item.size.toInt(), color = item.color, content = item.text,
                        ))
                    }.getOrNull() ?: return@forEach
                    advanced += parsed.advancedList
                    parsed.basList.forEach { script ->
                        runCatching { BasScenePainter(script) }.getOrNull()?.let { bas += it }
                    }
                }
            SpecialDanmakuProgram(advanced.sortedBy { it.startTimeMs }, bas.sortedBy { it.item.startTimeMs })
        }
    }
    val playbackState = playback.state.collectAsState()
    val context = LocalContext.current
    val taps = remember { BasVideoTargets() }
    DisposableEffect(ui.danmakuHit, taps) {
        ui.danmakuHit.captureSpecial = { window ->
            val target = taps.hit(window)
            target?.let { PlayerDanmakuPress({ activateBasVideoTarget(context, playback, it) }, {}) }
        }
        onDispose { ui.danmakuHit.captureSpecial = null }
    }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val opacity = page.settings.danmakuOpacity.coerceIn(0f, 1f)
    if (program.advanced.isEmpty() && program.bas.isEmpty()) return
    val advancedWindow = remember(program) {
        TimedDanmakuWindow(program.advanced, { it.startTimeMs }, { it.startTimeMs + it.durationMs })
    }
    val basWindow = remember(program) {
        TimedDanmakuWindow(program.bas, { it.item.startTimeMs }, { it.item.startTimeMs + it.item.durationMs - 1 })
    }
    Canvas(Modifier.fillMaxSize().onGloballyPositioned { taps.coordinates = it }) {
        val now = if (playbackState.value.playing) ui.frameMs.value else playbackState.value.positionMs
        taps.active = basWindow.update(now)
        val canvas = drawContext.canvas.nativeCanvas
        val clip = ui.danmakuHit.hotPresentation.value.heightPx.toFloat().coerceAtMost(size.height)
        canvas.save()
        canvas.clipRect(0f, 0f, size.width, size.height)
        drawAdvancedDanmaku(canvas, advancedWindow.update(now), now, paint, size.width, size.height, opacity)
        taps.clipTop = clip
        for (scene in taps.active) {
            scene.update(now - scene.item.startTimeMs, size.width, size.height, opacity, 1f, 5)
            scene.draw(canvas)
        }
        canvas.restore()
    }
}

private data class SpecialDanmakuProgram(
    val advanced: List<AdvancedDanmakuData> = emptyList(),
    val bas: List<BasScenePainter> = emptyList(),
)

private fun drawAdvancedDanmaku(canvas: AndroidCanvas, items: List<AdvancedDanmakuData>, now: Long,
    paint: Paint, width: Float, height: Float, opacity: Float) {
    for (item in items) {
        if (item.startTimeMs > now) break
        if (!item.isActive(now)) continue
        val progress = item.easing.transform(item.getTranslationProgress(now))
        val position = if (item.path.isNotEmpty()) item.getPathPointAt(progress)
            else com.android.purebilibili.danmaku.parser.BasPathPoint(
                item.startX + (item.endX - item.startX) * progress, item.startY + (item.endY - item.startY) * progress)
        paint.textSize = (item.fontSize * width / 672f).coerceAtLeast(1f)
        canvas.save()
        canvas.translate(position.x * width, position.y * height)
        canvas.rotate(item.rotateZ)
        canvas.scale(kotlin.math.cos(Math.toRadians(item.rotateY.toDouble())).toFloat(), 1f)
        drawSpecialText(canvas, paint, item.content, item.color, item.getAlphaAt(now) * opacity, !item.noStroke)
        canvas.restore()
    }
}

private class BasVideoTargets {
    var coordinates: LayoutCoordinates? = null
    var clipTop = 0f
    var active: List<BasScenePainter> = emptyList()
    fun hit(point: androidx.compose.ui.geometry.Offset): BasTarget? {
        val layout = coordinates?.takeIf { it.isAttached } ?: return null
        val local = layout.windowToLocal(point)
        if (local.x < 0 || local.x >= layout.size.width || local.y !in clipTop..<layout.size.height.toFloat()) return null
        for (i in active.indices.reversed()) active[i].hit(local.x, local.y)?.let { return it }
        return null
    }
}

private fun activateBasVideoTarget(context: android.content.Context, playback: PlaybackController, target: BasTarget) {
    val uri = android.net.Uri.Builder().scheme("https").authority("www.bilibili.com")
    when (target) {
        is BasTarget.Seek -> {
            playback.seekTo(target.timeMs.coerceIn(0, playback.refresh().durationMs.coerceAtLeast(0)))
            return
        }
        is BasTarget.Video -> {
            val id = target.bvid?.takeIf { Regex("BV[A-Za-z0-9]{10}").matches(it) }
                ?: target.aid?.takeIf { it > 0 }?.let { "av$it" } ?: return
            uri.appendPath("video").appendPath(id).appendQueryParameter("p", target.page.coerceAtLeast(1).toString())
                .appendQueryParameter("t", (target.timeMs.coerceAtLeast(0) / 1000).toString())
        }
        is BasTarget.Bangumi -> {
            val id = target.episodeId?.takeIf { it > 0 }?.let { "ep$it" }
                ?: target.seasonId?.takeIf { it > 0 }?.let { "ss$it" } ?: return
            uri.appendPath("bangumi").appendPath("play").appendPath(id)
                .appendQueryParameter("t", (target.timeMs.coerceAtLeast(0) / 1000).toString())
        }
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri.build())
    try {
        context.startActivity(intent)
        playback.pause()
    } catch (_: android.content.ActivityNotFoundException) {
        context.showToast("没有可打开关联视频的应用")
    }
}

private fun drawSpecialText(canvas: AndroidCanvas, paint: Paint, text: String, color: Int, opacity: Float, outline: Boolean) {
    val alpha = (opacity.coerceIn(0f, 1f) * 255).toInt()
    if (outline) {
        paint.color = alpha shl 24
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawText(text, 0f, -paint.ascent(), paint)
    }
    paint.style = Paint.Style.FILL
    paint.color = (alpha shl 24) or (color and 0xFFFFFF)
    canvas.drawText(text, 0f, -paint.ascent(), paint)
}
