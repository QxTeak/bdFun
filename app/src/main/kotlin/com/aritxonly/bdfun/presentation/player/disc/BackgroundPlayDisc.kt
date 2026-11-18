package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import androidx.compose.runtime.collectAsState

@Composable
internal fun BackgroundPlayDisc(settings: SettingsBindings, dependencies: DiscDependencies,
    modifier: Modifier = Modifier, size: Dp = 54.dp, outerSize: Dp? = null,
    breathEnabled: Boolean = true, shadowEnabled: Boolean = true, onOpenFallback: (VideoItem) -> Unit) {
    val runtime = rememberDiscRuntime(settings, dependencies, size, onOpenFallback)
    DiscLifecycleEffects(runtime)
    val motion = rememberDiscMotion(runtime)
    DiscExitEffect(runtime)
    DiscCoverEffects(runtime)
    DiscPlaybackEffects(runtime)
    val visual = rememberDiscVisual(runtime, motion).let {
        if (breathEnabled) it else it.copy(breathEnabled = false)
    }
    DiscSurface(runtime, visual, modifier, outerSize, shadowEnabled)
}

@Composable
private fun rememberDiscRuntime(settings: SettingsBindings, dependencies: DiscDependencies, size: Dp,
    onOpenFallback: (VideoItem) -> Unit): DiscRuntime {
    val context = LocalContext.current
    val oneTake = rememberOneTakeAnchor()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val radiusPx = with(density) { size.toPx() / 2f }
    val ui = remember { DiscPresentationStateHolder(dependencies.playback.state.value.cover) }
    val projection = remember(dependencies.playback) { observeDiscPlayback(dependencies.playback.state) }
    val initial = remember(dependencies.playback) {
        val snapshot = dependencies.playback.state.value
        DiscPlaybackState(snapshot.cover, snapshot.playback.playing)
    }
    val session by projection.collectAsState(initial)
    val coverPx = with(density) { (size * 0.60f).roundToPx() }
    val geometry = remember(density) { with(density) { DiscGestureGeometry(32.dp.toPx(), 8.dp.toPx(), 8.dp.toPx()) } }
    return remember(settings, dependencies, size, ui, scope, context, oneTake, coverPx, geometry, session, onOpenFallback) {
        DiscRuntime(settings, dependencies, size, ui, scope, context, oneTake, coverPx, geometry, session) {
            val base = dependencies.playback.video
            if (base != null) {
                val target = base.copy(
                    extras = base.extras.copy(
                        startPositionMs = dependencies.playback.positionMs.coerceAtLeast(0L),
                    ),
                )
                oneTake.open(context, target, radiusPx) { onOpenFallback(target) }
            }
        }
    }
}

@Composable
private fun rememberDiscMotion(runtime: DiscRuntime): DiscMotion {
    val ui = runtime.ui
    return DiscMotion(
        rememberSpringFloat(ui.dragTarget.x, stiffness = 520f, damping = 28f),
        rememberSpringFloat(ui.dragTarget.y, stiffness = 520f, damping = 28f),
        rememberManualProgress(if (ui.exiting) 1f else 0f, 240f),
    )
}

@Composable
private fun rememberDiscVisual(runtime: DiscRuntime, motion: DiscMotion): DiscVisual {
    val ui = runtime.ui
    val blurProgress = rememberManualProgress(target = if (ui.exiting || ui.pressed || ui.longPressActive) 1f else 0f,
        durationMs = 140f)
    val glassBlurDp = if (ui.coverBlur > 0.01f) 0f else blurProgress * 3f
    val breath01 = (0.5f + 0.5f * sin(ui.breathPhase * 2f * PI.toFloat())).coerceIn(0f, 1f)
    return DiscVisual(motion.dragX, motion.dragY, motion.exitProgress, glassBlurDp,
        runtime.settings.discBreathEnabled && runtime.session.playing, breath01,
        if (runtime.settings.discBreathCoverColor) ui.coverTintColor else Color.White)
}
