package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons

internal enum class DmIconKind { Fixed, Scroll, Color, Advanced, Count }

@Composable
internal fun DmTypeGlyph(kind: DmIconKind, tint: Color) {
    val icon = when (kind) {
        DmIconKind.Fixed -> LucideIcons.Captions
        DmIconKind.Scroll -> LucideIcons.ArrowLeftRight
        DmIconKind.Color -> LucideIcons.Palette
        DmIconKind.Advanced -> LucideIcons.GraphStar
        DmIconKind.Count -> LucideIcons.LampCeiling
    }
    AppGlyph(icon, tint, size = 22.dp)
}
