package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal class VideoActionPressStateHolder {
    var isPressing by mutableStateOf(false)
    var press by mutableFloatStateOf(0f)
    var longPressFired by mutableStateOf(false)
    var rippleHolding by mutableStateOf(false)
    var rippleBurst by mutableIntStateOf(0)
}
@Composable
internal fun VideoActionPressDecay(state: VideoActionPressStateHolder, kind: ActionKind) {
    val pressDecayMs = if (kind == ActionKind.Coin) 230f else 160f
    LaunchedEffect(state.isPressing) {
        if (state.isPressing) {
            state.press = 1f
        } else {
            // 首帧 press 本来就是 0，别把它“回弹”成 1 再落回去
            if (state.press <= 0.01f) return@LaunchedEffect
            val startNanos = System.nanoTime()
            while (true) {
                val t = ((System.nanoTime() - startNanos) / 1_000_000f / pressDecayMs)
                    .coerceIn(0f, 1f)
                state.press = 1f - t
                if (t >= 1f) break
                kotlinx.coroutines.delay(8)
            }
            state.press = 0f
        }
    }
}

@Composable
internal fun VideoActionLongPress(state: VideoActionPressStateHolder, onLongPress: (() -> Unit)?) {
    val haptics = VideoActionHaptics(LocalContext.current, com.aritxonly.teak.common.theme.LocalHapticsEnabled.current)
    LaunchedEffect(state.isPressing, onLongPress) {
        if (!state.isPressing) {
            state.rippleHolding = false
            return@LaunchedEffect
        }
        state.longPressFired = false
        if (onLongPress == null) return@LaunchedEffect
        delay(280)
        if (!state.isPressing) {
            state.rippleHolding = false
            return@LaunchedEffect
        }
        state.rippleHolding = true
        val start = System.nanoTime()
        var nextPulseNs = 0L
        while (state.isPressing && !state.longPressFired) {
            val now = System.nanoTime()
            val elapsed = (now - start) / 1_000_000f
            // 大震点必须落在小震的同一节拍上：小震在 0/320/640/960ms，
            // 1280ms 先判完成、不再补小震，于是最后一下小震到大震正好也是 320ms。
            if (elapsed >= 1280f) {
                state.longPressFired = true
                state.rippleHolding = false
                state.rippleBurst += 1
                haptics.strongVibrate()
                onLongPress()
            } else if (now >= nextPulseNs) {
                nextPulseNs = now + 320_000_000L
                haptics.pulseVibrate()
            }
            delay(16)
        }
    }
}
