package com.aritxonly.teak.common.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.aritxonly.teak.common.material.TeakGlassShadowTokens
import com.aritxonly.teak.common.material.teakGlassShadow
import com.aritxonly.teak.common.material.teakGlassShadowTokens

internal fun teakFloatingNavigationShadowTokens(isDark: Boolean): TeakGlassShadowTokens =
    teakGlassShadowTokens(isDark)

@androidx.compose.runtime.Composable
fun Modifier.teakFloatingNavigationShadow(
    shape: Shape,
    isDark: Boolean,
): Modifier {
    return teakGlassShadow(
        shape = shape,
        isDark = isDark,
    )
}
