package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CompletePage(onStart: () -> Unit) {
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val accent = MiuixTheme.colorScheme.primary
    val fallback = Brush.linearGradient(
        listOf(
            lerp(surface, accent, 0.25f),
            lerp(surface, accent, 0.06f),
            lerp(surface, accent, 0.19f),
        ),
    )
    val backdrop = rememberLayerBackdrop()
    val ui = remember { OnboardingHeroPresentationStateHolder() }
    val wave = ui.wave
    val wavePhase = ui.wavePhase
    val density = LocalDensity.current
    val diameter = with(density) { 100.dp.toPx() }
    val radius = diameter / 2f
    val dark = surface.luminance() < 0.5f
    val glint = if (dark) lerp(accent, Color.Black, 0.35f) else lerp(accent, Color.White, 0.70f)

    Box(modifier = Modifier.fillMaxSize().background(fallback)) {
        OnboardingBackground(playing = true, backdrop = backdrop)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BdFunMark(
                        markSize = 100.dp,
                        wave = wave,
                        wavePhase = wavePhase,
                        onWaveRelease = { ui.wavePhase = it },
                        interactive = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    CompletePageTitle(ui, radius, diameter, glint, accent)
                    Spacer(Modifier.height(36.dp))
                    CompletePageMessage(ui, radius, diameter, glint, accent)
                }
            }
            GuidePrimaryPill(
                text = "开始使用",
                modifier = Modifier.widthIn(max = 336.dp).fillMaxWidth(),
                onClick = onStart,
            )
            Spacer(Modifier.height(44.dp))
        }
    }
}

@Composable
private fun CompletePageTitle(
    ui: OnboardingHeroPresentationStateHolder, radius: Float, diameter: Float, glint: Color, accent: Color,
) {
    Text(
        text = "bdFun",
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val measured = coords.positionInParent().y +
                    coords.size.height / 2f - radius
                if (ui.titleDistance != measured) ui.titleDistance = measured
            }
            .graphicsLayer {
                val lift = waveLift(ui.wave.value, ui.titleDistance, diameter, ui.wavePhase)
                compositingStrategy = if (ui.wave.isRunning) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
                scaleX = 1f + lift * 0.055f
                scaleY = 1f + lift * 0.035f
                translationY = -lift * 7.dp.toPx()
            }
            .waveGlint(ui.wave, ui.titleDistance, diameter, ui.wavePhase, glint, accent),
        fontSize = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        color = MiuixTheme.colorScheme.onBackground,
    )
}

@Composable
private fun CompletePageMessage(
    ui: OnboardingHeroPresentationStateHolder, radius: Float, diameter: Float, glint: Color, accent: Color,
) {
    Text(
        text = "设置完成",
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val measured = coords.positionInParent().y +
                    coords.size.height / 2f - radius
                if (ui.doneDistance != measured) ui.doneDistance = measured
            }
            .graphicsLayer {
                val lift = waveLift(ui.wave.value, ui.doneDistance, diameter, ui.wavePhase)
                compositingStrategy = if (ui.wave.isRunning) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
                scaleX = 1f + lift * 0.045f
                scaleY = 1f + lift * 0.025f
                translationY = -lift * 5.dp.toPx()
            }
            .waveGlint(ui.wave, ui.doneDistance, diameter, ui.wavePhase, glint, accent),
        fontSize = 24.sp,
        textAlign = TextAlign.Center,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}
