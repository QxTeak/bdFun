package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import kotlinx.coroutines.delay

@Composable
fun FullscreenStatusLine(modifier: Modifier = Modifier) {

    val clock by produceState(initialValue = clockText()) {
        while (true) {
            value = clockText()
            delay(15_000)
        }
    }
    Box(modifier = modifier.height(19.dp)) {
        Text(
            text = clock,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
fun ViewerGlyph(tint: Color = Color.White.copy(alpha = 0.86f)) {
    AppGlyph(LucideIcons.Users, tint, size = 12.dp)
}
