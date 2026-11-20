package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.SubtitleCue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun hasNextPage(info: FullscreenPlayerInfo): Boolean {
    if (info.pages.size <= 1) return false
    val index = info.pages.indexOfFirst { it.cid == info.currentCid }
    return index >= 0 && index + 1 < info.pages.size
}

fun viewerText(info: FullscreenPlayerInfo): String {
    val text = info.onlineText.ifBlank {
        if (info.onlineCount > 0) compactCount(info.onlineCount) else ""
    }
    return if (text.isBlank()) "" else "${text}人正在看"
}

fun compactCount(value: Long): String {
    if (value < 10_000) return value.toString()
    if (value >= 100_000_000) return trimCount(value / 100_000_000.0) + "亿"
    return trimCount(value / 10_000.0) + "万"
}

internal fun trimCount(value: Double): String {
    val one = "%.1f".format(Locale.CHINA, value)
    return if (one.endsWith(".0")) one.dropLast(2) else one
}

fun trimSpeed(value: Float): String {
    val one = "%.2f".format(Locale.CHINA, value)
    return one.trimEnd('0').trimEnd('.')
}

// 全屏时钟每秒刷新一次：复用线程级 formatter，别每秒新建。
private val clockFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("HH:mm", Locale.CHINA)
}

fun clockText(): String = clockFormatter.get()!!.format(Date())

/** 字幕层：控件自动隐藏后仍然显示当前句。 */
@Composable
fun SubtitleOverlay(
    cues: List<SubtitleCue>,
    positionMs: Long,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {

    if (cues.isEmpty()) return
    val cue = remember(cues, positionMs) {
        cues.lastOrNull { positionMs >= it.fromMs && positionMs <= it.toMs }
    } ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = cue.text,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

fun formatPlayTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
