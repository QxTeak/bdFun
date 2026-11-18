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
import androidx.core.graphics.PathParser
import com.android.purebilibili.danmaku.parser.bas.BasTarget
import com.android.purebilibili.danmaku.parser.bas.text
import com.android.purebilibili.danmaku.parser.bas.number
import com.android.purebilibili.danmaku.parser.TimedDanmakuWindow
import com.android.purebilibili.danmaku.parser.DanmakuParser
import com.android.purebilibili.danmaku.parser.DanmakuProto
import com.android.purebilibili.danmaku.parser.AdvancedDanmakuData
import com.android.purebilibili.danmaku.parser.bas.BasDanmaku
import com.android.purebilibili.danmaku.parser.bas.BasElementType
import com.android.purebilibili.danmaku.parser.bas.BasMeasurer
import com.android.purebilibili.danmaku.parser.bas.BasTimeline
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.domain.DanmakuRuleMatcher
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
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
    LaunchedEffect(controller, aid, cid) { remote = controller.specialDanmaku(aid, cid) }
    LaunchedEffect(items, remote, state.rules, state.recalled) {
        program = withContext(Dispatchers.Default) {
            val rules = DanmakuRuleMatcher(state.rules.filter { it.kind == com.aritxonly.bdfun.core.domain.DanmakuRuleKind.Sender })
            val advanced = mutableListOf<AdvancedDanmakuData>()
            val bas = mutableListOf<BasVideoScene>()
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
                        runCatching { BasVideoScene(script) }.getOrNull()?.let { bas += it }
                    }
                }
            SpecialDanmakuProgram(advanced.sortedBy { it.startTimeMs }, bas.sortedBy { it.script.startTimeMs })
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
        TimedDanmakuWindow(program.bas, { it.script.startTimeMs }, { it.script.startTimeMs + it.script.durationMs - 1 })
    }
    Canvas(Modifier.fillMaxSize().onGloballyPositioned { taps.coordinates = it }) {
        taps.areas.clear()
        val now = if (playbackState.value.playing) ui.frameMs.value else playbackState.value.positionMs
        val canvas = drawContext.canvas.nativeCanvas
        val clip = ui.danmakuHit.hotPresentation.value.heightPx.toFloat().coerceAtMost(size.height)
        canvas.save()
        canvas.clipRect(0f, clip, size.width, size.height)
        drawAdvancedDanmaku(canvas, advancedWindow.update(now), now, paint, size.width, size.height, opacity)
        drawBasDanmaku(canvas, basWindow.update(now), now, paint, size.width, size.height, opacity, clip, taps)
        canvas.restore()
    }
}

private data class SpecialDanmakuProgram(
    val advanced: List<AdvancedDanmakuData> = emptyList(),
    val bas: List<BasVideoScene> = emptyList(),
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

private class BasVideoScene(val script: BasDanmaku) {
    val timeline = BasTimeline(script.program)
    val paths = timeline.states.mapNotNull { node ->
        if (node.element.type != BasElementType.PATH) return@mapNotNull null
        val path = runCatching { PathParser.createPathFromPathData(node.element.attributes.text("d")) }.getOrNull()
            ?: return@mapNotNull null
        node to path
    }.toMap()
}

private class BasVideoTargets {
    var coordinates: LayoutCoordinates? = null
    val areas = mutableListOf<Pair<android.graphics.RectF, BasTarget>>()
    fun hit(point: androidx.compose.ui.geometry.Offset): BasTarget? {
        val local = coordinates?.takeIf { it.isAttached }?.windowToLocal(point) ?: return null
        return areas.lastOrNull { it.first.contains(local.x, local.y) }?.second
    }
}

private fun drawBasDanmaku(canvas: AndroidCanvas, items: List<BasVideoScene>, now: Long,
    paint: Paint, width: Float, height: Float, opacity: Float, clip: Float, taps: BasVideoTargets) {
    for (scene in items) {
        val script = scene.script
        if (script.startTimeMs > now) break
        val elapsed = now - script.startTimeMs
        if (elapsed !in 0 until script.durationMs) continue
        val timeline = scene.timeline
        timeline.update(elapsed, width, height, BasMeasurer { state, _, _ ->
            paint.textSize = state.fontSize.coerceIn(1f, 256f)
            val path = scene.paths[state]
            val bounds = path?.let { android.graphics.RectF().apply { it.computeBounds(this, true) } }
            state.width = bounds?.right ?: paint.measureText(state.content)
            state.height = bounds?.bottom ?: paint.textSize
        })
        for (node in timeline.states) {
            if (!node.visible || node.alpha <= 0f) continue
            val path = scene.paths[node]
            if (path == null && node.content.isEmpty()) continue
            var x = node.x
            var y = node.y
            var parent = timeline.states.getOrNull(node.parentIndex)
            while (parent != null) {
                x += parent.x
                y += parent.y
                parent = timeline.states.getOrNull(parent.parentIndex)
            }
            paint.textSize = node.fontSize.coerceIn(1f, 256f)
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(node.rotateZ)
            canvas.scale(node.scale, node.scale)
            if (path == null) drawSpecialText(canvas, paint, node.content, node.color, node.alpha * opacity, true)
            else {
                paint.style = Paint.Style.FILL
                val alpha = (node.alpha * opacity * 255).toInt().coerceIn(0, 255)
                val color = node.element.attributes.number("fillColor", node.color.toDouble()).toInt()
                paint.color = (alpha shl 24) or (color and 0xFFFFFF)
                canvas.drawPath(path, paint)
            }
            canvas.restore()
            node.element.target?.let {
                val bounds = android.graphics.RectF(x, maxOf(y, clip), x + node.width * node.scale, y + node.height * node.scale)
                if (!bounds.isEmpty) taps.areas += bounds to it
            }
        }
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
