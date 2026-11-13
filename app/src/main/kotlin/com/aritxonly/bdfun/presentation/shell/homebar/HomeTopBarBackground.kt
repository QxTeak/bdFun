package com.aritxonly.bdfun.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.teakProgressiveBarBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun androidx.compose.foundation.layout.BoxScope.HomeTopBarBackground(classic: Boolean) {
    val classicSurface = MiuixTheme.colorScheme.surfaceContainer
    val classicLine = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                if (classic) {
                    Modifier
                        .background(classicSurface)
                        .drawBehind {
                            drawLine(
                                color = classicLine,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                } else {
                    Modifier.teakProgressiveBarBlur()
                },
            ),
    )

}
