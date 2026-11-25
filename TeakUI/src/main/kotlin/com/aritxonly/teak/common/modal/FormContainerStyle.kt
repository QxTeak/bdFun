// Migrated from Deadliner 3b561656 working tree; see docs/deadliner-modal-migration.md.
package com.aritxonly.teak.common.modal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.aritxonly.teak.common.modal.LocalTextFieldContainerColors
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleSurface

/** Set after the popup boundary so every form in any Sheet gets the Sheet semantic role. */
internal val LocalSheetFormContainers = staticCompositionLocalOf { false }

@Composable
internal fun formContainerColor(fallback: Color): Color {
    val colors = LocalTextFieldContainerColors.current
    return if (LocalSheetFormContainers.current) colors?.textFieldSheetContainer ?: fallback.copy(alpha = 0.65f)
        else colors?.textFieldContainer ?: fallback
}

/** Match MIUIX TextField's translucent fill path, including its continuous rounded corners. */
@Composable
internal fun Modifier.formContainerBackground(cornerRadius: Dp): Modifier = squircleSurface(Color.Transparent, cornerRadius).squircleBackground(
    color = formContainerColor(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.secondaryContainer),
    cornerRadius = cornerRadius,
)


