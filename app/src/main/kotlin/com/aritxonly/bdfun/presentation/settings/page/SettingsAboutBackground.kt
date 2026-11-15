/*
 * Adapted from Deadliner (MIT), Copyright (c) 2024 Aritx Zhou.
 * Full upstream drawing closure; provenance/license: docs/SETTINGS_DEADLINER_PROVENANCE.md.
 */
package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

@Composable
internal fun SettingsAboutFloatingBackground(
    modifier: Modifier = Modifier,
    alpha: () -> Float,
    animationsEnabled: Boolean,
) {
    val (horizontalOffset, verticalOffset, accentOffset) = rememberAboutOffsets(animationsEnabled)
    val primary = MaterialTheme.colorScheme.primary.toVividGlowColor()
    val secondary = MaterialTheme.colorScheme.secondary.toVividGlowColor()
    val tertiary = MaterialTheme.colorScheme.tertiary.toVividGlowColor()
    val surface = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        drawRect(color = surface)
        val alpha = alpha().coerceIn(0f, 1f)
        if (alpha > 0f) {
            val glowRadius = maxOf(size.width, size.height) * 0.9f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.22f * alpha), Color.Transparent),
                    center = Offset(
                        x = size.width * (0.18f + horizontalOffset.value),
                        y = size.height * (0.22f + verticalOffset.value),
                    ),
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = Offset(
                    x = size.width * (0.18f + horizontalOffset.value),
                    y = size.height * (0.22f + verticalOffset.value),
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = 0.18f * alpha), Color.Transparent),
                    center = Offset(
                        x = size.width * (0.86f - horizontalOffset.value),
                        y = size.height * (0.66f - verticalOffset.value),
                    ),
                    radius = glowRadius,
                ),
                radius = glowRadius * 0.86f,
                center = Offset(
                    x = size.width * (0.86f - horizontalOffset.value),
                    y = size.height * (0.66f - verticalOffset.value),
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiary.copy(alpha = 0.16f * alpha), Color.Transparent),
                    center = Offset(
                        x = size.width * (0.52f + accentOffset.value),
                        y = size.height * (0.96f - accentOffset.value),
                    ),
                    radius = glowRadius * 0.7f,
                ),
                radius = glowRadius * 0.72f,
                center = Offset(
                    x = size.width * (0.52f + accentOffset.value),
                    y = size.height * (0.96f - accentOffset.value),
                ),
            )
        }
    }
}

private fun Color.toVividGlowColor(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[1] = (hsl[1] * 1.55f).coerceIn(0.56f, 0.92f)
    hsl[2] = if (hsl[2] < 0.5f) {
        (hsl[2] + 0.14f).coerceAtMost(0.68f)
    } else {
        (hsl[2] - 0.06f).coerceAtLeast(0.36f)
    }
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
private fun rememberAboutOffsets(animationsEnabled: Boolean): Triple<State<Float>, State<Float>, State<Float>> {
    val transition = if (animationsEnabled) rememberInfiniteTransition(label = "aboutFloatingBackground") else null
    val horizontalOffset = transition?.animateFloat(
        initialValue = -0.12f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutBackgroundHorizontalOffset",
    ) ?: rememberUpdatedState(0f)
    val verticalOffset = transition?.animateFloat(
        initialValue = 0.08f,
        targetValue = -0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutBackgroundVerticalOffset",
    ) ?: rememberUpdatedState(0f)
    val accentOffset = transition?.animateFloat(
        initialValue = -0.08f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutBackgroundAccentOffset",
    ) ?: rememberUpdatedState(0f)
    return Triple(horizontalOffset, verticalOffset, accentOffset)
}
