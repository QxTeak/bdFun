package com.aritxonly.bdfun.ui

import android.app.Activity
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons

@Composable
internal fun LiveGestureIconTip(
    brightness: Boolean,
    value: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveGestureSymbol(brightness)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFB7299)),
            )
        }
    }
}


internal fun liveCurrentBrightness(activity: Activity?): Float {
    val window = activity?.window ?: return 0.5f
    val explicit = window.attributes.screenBrightness
    if (explicit >= 0f) return explicit
    // -1 = 跟随系统；从系统当前亮度开始调，而不是固定 50%。
    val raw = runCatching {
        android.provider.Settings.System.getInt(
            activity.contentResolver,
            android.provider.Settings.System.SCREEN_BRIGHTNESS,
        )
    }.getOrDefault(128)
    return (raw / 255f).coerceIn(0.02f, 1f)
}


internal fun liveApplyBrightness(activity: Activity?, value: Float) {
    val window = activity?.window ?: return
    window.attributes = window.attributes.apply {
        screenBrightness = value.coerceIn(0.02f, 1f)
    }
}


internal fun liveCurrentVolumeRatio(audio: AudioManager): Float {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}


internal fun liveSetVolumeRatio(audio: AudioManager, ratio: Float) {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    audio.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        (ratio * max).toInt().coerceIn(0, max),
        0,
    )
}

/* ------------------------------------------------------------------ */
/* 弹幕设置：复用视频播放器的 DanmakuSettingsContent                      */
/* ------------------------------------------------------------------ */


@Composable
private fun LiveGestureSymbol(brightness: Boolean) {
    AppGlyph(if (brightness) LucideIcons.Sun else LucideIcons.Volume1, Color.White, size = 24.dp)
}
