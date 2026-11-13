package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideArrowButton(
    backdrop: LayerBackdrop,
    onClick: () -> Unit,
) {
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val shader = remember { isRuntimeShaderSupported() }
    val scrim = lerp(
        MiuixTheme.colorScheme.onSurfaceContainer,
        MiuixTheme.colorScheme.onSurface,
        0.35f,
    ).copy(alpha = 0.34f)
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .then(
                if (shader) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = CircleShape,
                        blurRadius = 56f,
                        colors = BlurColors(blendColors = guideBlendColors()),
                    )
                } else {
                    Modifier
                },
            )
            .background(scrim, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.width(31.dp).height(22.dp)) {
            val middle = size.height / 2f
            val tip = size.width * 0.96f
            val tail = size.width * 0.04f
            val wing = size.height * 0.42f
            drawLine(surface, Offset(tail, middle), Offset(tip, middle), 2.6.dp.toPx(), StrokeCap.Round)
            drawLine(surface, Offset(tip - wing, middle - wing), Offset(tip, middle), 2.6.dp.toPx(), StrokeCap.Round)
            drawLine(surface, Offset(tip - wing, middle + wing), Offset(tip, middle), 2.6.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun guideBlendColors(): List<BlendColorEntry> {
    val dark = MiuixTheme.colorScheme.surfaceContainer.luminance() < 0.5f
    return remember(dark) {
        if (dark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
            )
        }
    }
}
