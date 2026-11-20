package com.aritxonly.bdfun.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.TextPaint
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_SCROLL
import com.android.purebilibili.danmaku.engine.DanmakuItem

/** 直播弹幕里的一个片段：普通文字或图片表情。 */
private sealed interface LiveDmSegment {
    data class Text(val value: String) : LiveDmSegment
    data class Emote(val token: String, val url: String) : LiveDmSegment
}

/**
 * 把带表情的直播弹幕画成一张透明 Bitmap，再交给 danmaku-engine 的图片弹幕通道。
 *
 * 引擎本身只接受纯文本或位图；表情弹幕在 BiliPai 里也是先光栅化成 Bitmap，
 * 这样文字和表情能在同一条滚动弹幕里混排，轨道分配和追尾判断不受影响。
 */
internal suspend fun buildLiveEmoteDanmaku(
    context: Context,
    text: String,
    textColor: Int,
    textSizePx: Float,
    emotes: Map<String, String>,
    emoteHeights: Map<String, Int> = emptyMap(),
): DanmakuItem? {
    if (text.isBlank() || emotes.isEmpty()) return null
    val segments = splitLiveEmotes(text, emotes)
    if (segments.none { it is LiveDmSegment.Emote }) return null

    val paint = TextPaint().apply {
        this.textSize = textSizePx
        this.color = textColor
        this.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        this.isAntiAlias = true
        this.setShadowLayer(
            (textSizePx / 8f).coerceIn(2f, 5f),
            0f,
            0f,
            android.graphics.Color.BLACK,
        )
    }
    val metrics = paint.fontMetrics
    val textHeight = metrics.descent - metrics.ascent
    val defaultEmoteHeight = textSizePx * 1.3f


    var totalWidth = 0f
    val rendered = segments.map { segment ->
        when (segment) {
            is LiveDmSegment.Text -> {
                val width = paint.measureText(segment.value).coerceAtLeast(1f)
                totalWidth += width
                Rendered(segment.value, null, width, textHeight)
            }

            is LiveDmSegment.Emote -> {
                val requestedHeight = emoteHeights[segment.token]
                    ?.takeIf { it > 0 }
                    ?.let { it * context.resources.displayMetrics.density }
                    ?: defaultEmoteHeight
                val bitmap = loadLiveEmoteBitmap(context, segment.url, requestedHeight.toInt())
                if (bitmap != null && bitmap.height > 0) {
                    val ratio = (bitmap.width.toFloat() / bitmap.height)
                        .coerceIn(0.25f, 4f)
                    val height = requestedHeight.coerceIn(textSizePx * 0.8f, textSizePx * 2f)
                    val width = height * ratio
                    totalWidth += width + 4f
                    Rendered(null, bitmap, width, height)
                } else {
                    val width = paint.measureText(segment.token).coerceAtLeast(1f)
                    totalWidth += width
                    Rendered(segment.token, null, width, textHeight)
                }
            }
        }
    }
    if (totalWidth <= 0f) return null

    return rasterizeLiveEmote(rendered, paint, textHeight, defaultEmoteHeight, totalWidth)
}

private suspend fun loadLiveEmoteBitmap(context: Context, url: String, targetHeightPx: Int): Bitmap? {
    if (url.isBlank()) return null
    val size = targetHeightPx.coerceIn(24, 256)
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(size)
        .allowHardware(false)
        .crossfade(false)
        .build()
    val result = runCatching { Coil.imageLoader(context).execute(request) }.getOrNull()
    val drawable = (result as? SuccessResult)?.drawable ?: return null
    return runCatching {
        if (drawable is BitmapDrawable) drawable.bitmap else drawable.toBitmap()
    }.getOrNull()
}

/**
 * 表情弹幕的正则按「当前直播间表情快照」缓存：每条弹幕都重新排序 + 编译
 * 在刷屏直播间里是实打实的卡顿来源；快照替换（换房间/刷新表情）时自然失效。
 */
private class CompiledLiveEmotes(
    val source: Map<String, String>,
    val keys: Set<String>,
    val pattern: Regex,
)
private var compiledLiveEmotes: CompiledLiveEmotes? = null

@Synchronized
private fun liveEmoteMatcher(emotes: Map<String, String>): CompiledLiveEmotes {
    compiledLiveEmotes?.let { if (it.source === emotes) return it }
    val keys = emotes.filter { it.key.isNotEmpty() && it.value.isNotBlank() }.keys
    val pattern = Regex(
        (keys.sortedByDescending { it.length }.map { Regex.escape(it) } + "\\[(.*?)\\]")
            .joinToString("|"),
    )
    return CompiledLiveEmotes(emotes, keys, pattern).also { compiledLiveEmotes = it }
}

private fun splitLiveEmotes(text: String, emotes: Map<String, String>): List<LiveDmSegment> {
    val compiled = liveEmoteMatcher(emotes)
    val keys = compiled.keys
    if (keys.isEmpty()) return listOf(LiveDmSegment.Text(text))
    if (text in keys) return listOf(LiveDmSegment.Emote(text, emotes.getValue(text)))
    val result = mutableListOf<LiveDmSegment>()
    var offset = 0
    compiled.pattern.findAll(text).forEach { match ->
        if (match.range.first > offset) {
            result += LiveDmSegment.Text(text.substring(offset, match.range.first))
        }
        val token = match.value
        val bare = token.removePrefix("[").removeSuffix("]")
        val key = when {
            token in keys -> token
            bare in keys -> bare
            else -> null
        }
        if (key != null) {
            result += LiveDmSegment.Emote(key, emotes.getValue(key))
        } else {
            result += LiveDmSegment.Text(token)
        }
        offset = match.range.last + 1
    }
    if (offset < text.length) {
        result += LiveDmSegment.Text(text.substring(offset))
    }
    return result
}

private data class Rendered(
        val text: String?,
        val bitmap: Bitmap?,
        val width: Float,
        val height: Float,
    )


private fun rasterizeLiveEmote(rendered: List<Rendered>, paint: TextPaint, textHeight: Float,
    defaultEmoteHeight: Float, totalWidth: Float): DanmakuItem {
    val width = totalWidth.toInt().coerceIn(1, 2048)
    val height = (maxOf(textHeight, defaultEmoteHeight) * 1.25f)
        .toInt()
        .coerceIn(1, 256)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val baseline = (height - paint.fontMetrics.descent - paint.fontMetrics.ascent) / 2f
    var x = 0f
    rendered.forEach { part ->
        if (part.bitmap != null) {
            val top = (height - part.height) / 2f
            canvas.drawBitmap(
                part.bitmap,
                null,
                RectF(x, top, x + part.width, top + part.height),
                null,
            )
            x += part.width + 4f
        } else {
            canvas.drawText(part.text.orEmpty(), x, baseline, paint)
            x += part.width
        }
    }

    return DanmakuItem().apply {
        layerType = DANMAKU_LAYER_SCROLL
        this.bitmap = bitmap
        bitmapWidth = width.toFloat()
        bitmapHeight = height.toFloat()
        isSelf = false
    }
}
