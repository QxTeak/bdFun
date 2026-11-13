package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.BrandPinkDeep
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
                Canvas(modifier = Modifier.size(20.dp)) {
                    val w = size.width
                    val h = size.height
                    val gap = w * 0.12f
                    val cell = (w - gap) / 2f
                    val r = CornerRadius(cell * 0.22f)
                    listOf(
                        Offset(0f, 0f),
                        Offset(cell + gap, 0f),
                        Offset(0f, cell + gap),
                        Offset(cell + gap, cell + gap),
                    ).forEach { topLeft ->
                        drawRoundRect(
                            color = Color.White,
                            topLeft = topLeft,
                            size = Size(cell, cell),
                            cornerRadius = r,
                        )
                    }
                }
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
