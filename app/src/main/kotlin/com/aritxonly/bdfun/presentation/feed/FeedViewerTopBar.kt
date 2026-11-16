package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*

@Composable
internal fun FeedViewerTopBar(dismissAnimated: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppGlyph(MiuixIcons.Close, size = 26.dp, tint = Color.White,

            modifier = Modifier
                .clip(CircleShape)
                .clickable(interactionSource = null, indication = null, onClick = dismissAnimated)
                .padding(6.dp))
        Spacer(Modifier.weight(1f))
        AppGlyph(MiuixIcons.More, size = 22.dp, tint = Color.White,

            modifier = Modifier.padding(6.dp))
    }
}
