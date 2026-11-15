package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DeveloperRow(
    name: String,
    avatar: Int,
    url: String,
    onOpen: (String) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    top.yukonga.miuix.kmp.preference.ArrowPreference(
        title = name,
        startAction = {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(avatar),
                contentDescription = name,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh),
            )
        },
        onClick = { onOpen(url) },
    )
}

@Composable
internal fun GlobalFontScaleRow(value: Float, onValueChange: (Float) -> Unit, title: String = "全局字号") {
    val min = 0.8f
    val max = 1.2f
    val ui = remember(value) { SettingsFontScalePresentationStateHolder(value) }
    top.yukonga.miuix.kmp.preference.SliderPreference(
        value = ui.value,
        onValueChange = {
            ui.value = if (kotlin.math.abs(it - 1f) < 0.025f) 1f else it
        },
        title = title,
        valueText = "${(ui.value * 100).toInt()}%",
        valueRange = min..max,
        keyPoints = listOf(1f),
        showKeyPoints = false,
        magnetThreshold = 0.035f,
        onValueChangeFinished = { onValueChange(ui.value) },
    )
}
