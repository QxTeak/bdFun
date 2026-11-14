package com.aritxonly.bdfun.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommentPictures(urls: List<String>, onOpen: (Int, Rect, Float, List<Rect?>) -> Unit) {
    val colors = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val radiusPx = with(density) { CommentPictureRadius.toPx() }
    // 缩略图在窗口里的矩形要交给查看器做共享元素入场，所以这里逐张记录。
    val bounds = remember { mutableMapOf<Int, Rect>() }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        urls.take(3).forEachIndexed { index, url ->
            AsyncImage(
                // 缩略图按 96dp 缩小、但保持原始比例；查看器用同一张源图做中心裁切。
                model = rememberImageRequest(
                    com.aritxonly.bdfun.core.common.biliImageThumbUrl(url, 240),
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(CommentPictureRadius))
                    .background(colors.surfaceContainerHigh)
                    .onGloballyPositioned { bounds[index] = it.boundsInRoot() }
                    .clickable(interactionSource = null, indication = null) {
                        onOpen(index, bounds[index] ?: Rect.Zero, radiusPx, urls.indices.map { bounds[it] })
                    },
            )
        }
        if (urls.size > 3) {
            Text(
                text = "+${urls.size - 3}",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
    }
}

private val CommentPictureRadius = 8.dp
