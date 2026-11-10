package com.aritxonly.teak.common.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.blur.Backdrop

object TeakGlassFloatingActionButtonDefaults {
    val Size = 58.dp
    val IconSize = 30.dp
}

/** A colored soft-glass FAB for immersive compact surfaces. */
@Composable
fun TeakGlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    size: Dp = TeakGlassFloatingActionButtonDefaults.Size,
    neutralTint: Boolean = false,
    backdrop: Backdrop? = LocalTeakBackdrop.current,
    content: @Composable () -> Unit,
) {
    val tintProgress by animateFloatAsState(if (neutralTint) 1f else 0f,
        spring(dampingRatio = 0.78f, stiffness = 380f), label = "fabNeutralTint")
    val animatedContentColor by animateColorAsState(contentColor,
        spring(dampingRatio = 0.78f, stiffness = 380f), label = "fabContentTint")

    TeakGlassIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        tint = containerColor.copy(alpha = 1f),
        tintStyle = GlassTintStyle.SeedChromatic,
        neutralTintProgress = tintProgress,
        backdrop = backdrop,
    ) {
        CompositionLocalProvider(LocalContentColor provides animatedContentColor) {
            content()
        }
    }
}
