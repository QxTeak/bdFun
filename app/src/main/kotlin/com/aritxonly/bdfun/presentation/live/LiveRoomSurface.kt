package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.model.live.LiveRoomInfo
import com.kyant.shapes.Capsule

@Composable
internal fun LivePlayerSurface(
    player: com.aritxonly.bdfun.feature.live.presentation.LiveVideoOutput,
    modifier: Modifier = Modifier,
) {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier.background(Color.Black),
        factory = { ctx -> player.createView(ctx) },
        update = { view -> player.bindView(view) },
        onRelease = { view -> player.releaseView(view) },
    )
}

/* ------------------------------------------------------------------ */
/* 播放器占位：接真实流之前，先用封面 + 渐变撑住画面                      */
/* ------------------------------------------------------------------ */


@Composable
internal fun LiveVideoPlaceholder(
    room: LiveRoomInfo,
    statusText: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black)) {
        BackdropGlow(room, Modifier.fillMaxSize())
        // 画面上不再显示「直播中」角标，只在左下角保留分区文字
        if (room.areaText.isNotBlank()) {
            Text(
                text = room.areaText,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.2.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 12.dp),
            )
        }
        Text(
            text = statusText ?: "正在连接直播…",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
        )
    }
}

/** 直播间背景：和 BiliPai 的 LiveRoomBackground 完全一致——铺满裁切 + 60% 透明度，不叠渐变。 */

@Composable
internal fun BackdropGlow(room: LiveRoomInfo, modifier: Modifier = Modifier) {
    Box(modifier) {
        if (room.cover.isNotBlank()) {
            AsyncImage(
                model = room.cover,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.6f },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 房间标题 + 关注                                                      */
/* ------------------------------------------------------------------ */


@Composable
internal fun FollowButton(
    following: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    val bg = if (following) Color.White.copy(alpha = 0.14f) else BrandPink
    Box(
        modifier = Modifier
            .clip(Capsule())
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = if (compact) 11.dp else 16.dp,
                vertical = if (compact) 3.dp else 7.dp,
            ),
    ) {
        Text(
            text = if (following) "已关注" else "关注",
            color = if (following) Color.White.copy(alpha = 0.8f) else Color.White,
            fontSize = if (compact) 12.sp else 14.4.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/* ------------------------------------------------------------------ */
/* 弹幕列表                                                            */
/* ------------------------------------------------------------------ */
