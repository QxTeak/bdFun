package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.BrandPinkDeep
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AllFeedAvatar(selected: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val ring = rememberManualProgress(if (selected) 1f else 0f, 200f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.55f),
                                BrandPink,
                                BrandPinkDeep,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AppGlyph(MiuixIcons.All, Color.White, size = 20.dp)
            }
            if (ring > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            2.dp,
                            colors.primary.copy(alpha = ring.coerceIn(0f, 1f)),
                            CircleShape,
                        ),
                )
            }
        }
        Text(
            text = "全部动态",
            color = colors.onBackgroundVariant,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
