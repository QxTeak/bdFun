package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope

@Immutable
internal data class ActivityPageOptions(val backRevealed: Boolean? = null, val showBackButton: Boolean = true,
    val title: String? = null, val limitContentWidth: Boolean = true, val showTopBar: Boolean = true,
    val showScrollTop: Boolean = true, val titleAlwaysVisible: Boolean = false,
    val topBarMaterialProgress: Float? = null, val barBlurFraction: Float = 1f, val topBarGlyphColor: Color? = null)
@Immutable
internal data class ActivityDiscOptions(val showBackgroundDisc: Boolean = true,
    val discLift: Dp = 0.dp, val discEndPadding: Dp? = null)
@Immutable
internal data class ActivityPageSlots(
    val topBarLeading: @Composable RowScope.() -> Unit = {},
    val topBarCenter: @Composable BoxScope.() -> Unit = {},
    val topBarActions: @Composable RowScope.() -> Unit = {},
    val overlay: @Composable BoxScope.() -> Unit = {},
    val menuOverlay: @Composable BoxScope.() -> Unit = {},
)
