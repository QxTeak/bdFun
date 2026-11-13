package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.R
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BdFunMark(
    markSize: Dp = 100.dp,
    wave: Animatable<Float, AnimationVector1D>,
    wavePhase: Float,
    onWaveRelease: (Float) -> Unit,
    interactive: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val ui = remember { OnboardingMarkPresentationStateHolder() }
    val pressScale by animateFloatAsState(
        targetValue = if (ui.pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f),
        label = "mark-press",
    )
    val breathe = rememberInfiniteTransition(label = "mark-breathe")
    val breath = breathe.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "mark-breath",
    )

    LaunchedEffect(ui.boosted) {
        if (ui.boosted) {
            onWaveRelease(0f)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                wave.snapTo(0f)
                wave.animateTo(1f, tween(durationMillis = 1150, easing = FastOutSlowInEasing))
            }
        }
    }

    val pressure = onboardingMarkPressure(interactive, ui)

    val tint = MiuixTheme.colorScheme.onSurface
    val airflow = lerp(tint, MiuixTheme.colorScheme.primary, 0.55f)

    Box(
        modifier = Modifier
            .size(markSize)
            .then(pressure)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        contentAlignment = Alignment.Center,
    ) {
        // 涟漪和归元一样画在图标外面一圈，只在长按触发时出现。
        if (wave.isRunning) {
            Canvas(Modifier.requiredSize(markSize * 3.8f)) {
                val progress = wave.value
                val diameter = markSize.toPx()
                drawWave(airflow, progress, 0.46f, diameter, wavePhase)
                drawWave(airflow, (progress - 0.2f) / 0.8f, 0.10f, diameter, wavePhase)
            }
        }
        // 前景图层四周自带约 16% 透明留白，放大 1.42 倍后图形正好落在 100dp 里。
        OnboardingMarkImage(markSize, breath)
    }
}

@Composable
private fun OnboardingMarkImage(markSize: Dp, breath: androidx.compose.runtime.State<Float>) {
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = "bdFun",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(markSize * 1.42f)
            .graphicsLayer {
                val s = 1f + 0.012f * breath.value
                scaleX = s
                scaleY = s
                translationY = (-1.5f * breath.value).dp.toPx()
            },
    )
}

private fun onboardingMarkPressure(interactive: Boolean, ui: OnboardingMarkPresentationStateHolder): Modifier = if (interactive) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    ui.pressed = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        ui.pressed = false
                        ui.boosted = false
                    }
                },
                onLongPress = { ui.boosted = true },
            )
        }
    } else {
        Modifier
    }
