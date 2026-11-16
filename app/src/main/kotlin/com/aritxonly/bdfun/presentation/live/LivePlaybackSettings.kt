package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*

@Composable
internal fun LiveSettingsOverlay(
    landscape: Boolean,
    qualities: List<Pair<Int, String>>,
    currentQuality: Int,
    onSelectQuality: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (landscape) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val panelWidth = (maxWidth.value * 0.41f).dp.coerceAtLeast(360.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.25f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.25f),
                            0.58f to Color.Black.copy(alpha = 0.74f),
                            0.68f to Color.Black.copy(alpha = 0.95f),
                            1f to Color.Black,
                        ),
                    )
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(panelWidth)
                    .fillMaxHeight()
                    .clickable(interactionSource = null, indication = null) {}
                    .navigationBarsPadding()
                    .padding(start = 32.dp, end = 30.dp, top = 30.dp, bottom = 18.dp),
            ) {
                LiveSettingsContent(
                    qualities = qualities,
                    currentQuality = currentQuality,
                    onSelectQuality = onSelectQuality,
                    onClose = onDismiss,
                    scrollable = true,
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Color(0xFF141519))
                    .clickable(interactionSource = null, indication = null) {}
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                LiveSettingsContent(
                    qualities = qualities,
                    currentQuality = currentQuality,
                    onSelectQuality = onSelectQuality,
                    onClose = onDismiss,
                    scrollable = false,
                )
            }
        }
    }
}


@Composable
internal fun LiveSettingsContent(
    qualities: List<Pair<Int, String>>,
    currentQuality: Int,
    onSelectQuality: (Int) -> Unit,
    onClose: () -> Unit,
    scrollable: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "播放设置",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(interactionSource = null, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            LiveCloseGlyph()
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = "清晰度",
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = if (scrollable) 480.dp else 320.dp)
            .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
    ) {
        if (qualities.isEmpty()) {
            Text(
                text = "暂无可用清晰度",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
        qualities.forEach { (qn, name) ->
            val active = qn == currentQuality
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) BrandPink.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable(interactionSource = null, indication = null) {
                        onSelectQuality(qn)
                    }
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = name,
                    color = if (active) BrandPink else Color.White,
                    fontSize = 15.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                if (active) LiveCheckGlyph(BrandPink)
            }
        }
    }
}


@Composable
internal fun LiveCloseGlyph(tint: Color = Color.White.copy(alpha = 0.7f)) {
    AppGlyph(MiuixIcons.Close, tint, size = 18.dp)
}


@Composable
internal fun LiveCheckGlyph(tint: Color = Color.White) {
    AppGlyph(MiuixIcons.Ok, tint, size = 15.dp)
}
