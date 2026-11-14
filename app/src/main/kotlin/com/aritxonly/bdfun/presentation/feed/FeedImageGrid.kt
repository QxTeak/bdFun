package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FeedImageGrid(
    images: List<String>,
    /** origins 与 images 同序：查看器靠它把每一页分别收拢回自己的缩略图。 */
    onOpen: (String, Rect, Float, List<Rect?>) -> Unit,
) {
    if (images.isEmpty()) return
    val colors = MiuixTheme.colorScheme
    val density = androidx.compose.ui.platform.LocalDensity.current
    val singleRadiusPx = with(density) { 12.dp.toPx() }
    val gridRadiusPx = with(density) { 10.dp.toPx() }
    val bounds = remember { FeedImageBoundsStateHolder() }.bounds
    if (images.size == 1) {
        val url = images.first()
        AsyncImage(
            // 列表里只要一张裁好的缩略图；交给查看器的是原始 url（不裁切）。
            model = rememberImageRequest(biliImageGridUrl(url)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(ContinuousCornerShape(12.dp))
                .background(colors.surfaceContainerHigh)
                .onGloballyPositioned { bounds[url] = it.boundsInRoot() }
                .clickable(interactionSource = null, indication = null) {
                    onOpen(url, bounds[url] ?: Rect.Zero, singleRadiusPx, images.map { bounds[it] })
                },
        )
        return
    }
    images.take(9).chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            row.forEach { url ->
                AsyncImage(
                    model = rememberImageRequest(biliImageGridUrl(url)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(ContinuousCornerShape(10.dp))
                        .background(colors.surfaceContainerHigh)
                        .onGloballyPositioned { bounds[url] = it.boundsInRoot() }
                        .clickable(interactionSource = null, indication = null) {
                            onOpen(url, bounds[url] ?: Rect.Zero, gridRadiusPx, images.map { bounds[it] })
                        },
                )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** 主动态和转发动态共用同一套大卡视频布局。 */

/**
 * 缩略图请求：按显示宽度缩小的**未裁切**原图。
 *
 * 不能再用 `_1c`（服务端居中裁 16:9）：列表再裁一次、查看器按原图裁一次，
 * 两次结果只有 16:9 的图才一致，其他比例收放都会错位。
 * 现在两边都从同一张原图做中心裁切，共用元素动画才对得上。
 */
private fun biliImageGridUrl(url: String): String =
    com.aritxonly.bdfun.core.common.biliImageThumbUrl(url, 720)
