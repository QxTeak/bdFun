package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.TeakGlassPanel
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.application.*

@Composable
internal fun EmojiGlassPanel(layout: EmojiGridLayout, spring: Float, onPick: (String) -> Unit,
    tabs: @Composable () -> Unit) {
    val panelHeight = layout.panelHeight
    TeakGlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .graphicsLayer {
                alpha = spring.coerceIn(0f, 1f)
                scaleX = 0.82f + 0.18f * spring
                scaleY = 0.82f + 0.18f * spring
                translationY = (1f - spring) * 10f
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            }
            .dropShadow(
                shape = RoundedCornerShape(24.dp),
                shadow = androidx.compose.ui.graphics.shadow.Shadow(
                    radius = 8.dp,
                    color = Color.Black,
                    alpha = 0.10f,
                ),
            ),
        shape = RoundedCornerShape(24.dp),
        // 和设置二级菜单/投币面板同一套底灰
        tintAlpha = 0.3f,
    ) {
        Column(Modifier.fillMaxSize().clipToBounds()) {
            tabs()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                androidx.compose.runtime.key(layout.columns, layout.cells) { EmojiGridContent(layout, onPick) }
            }
        }
    }
}

@Composable
private fun EmojiGridContent(layout: EmojiGridLayout, onPick: (String) -> Unit) {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(layout.columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = layout.hPad, vertical = layout.vPad),
        horizontalArrangement = Arrangement.spacedBy(layout.gap),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        items(layout.cells.size) { index -> EmojiGridCell(layout.cells[index], layout, onPick) }
    }
}

@Composable
private fun EmojiGridCell(item: EmojiCell, layout: EmojiGridLayout, onPick: (String) -> Unit) {
    val preview = remember(item) { EmojiPreviewStateHolder() }
    if (preview.visible && item.url != null) EmojiPreview(item) { preview.visible = false }
    Box(
        modifier = Modifier
            .width(layout.cell).height(layout.rowHeight).clipToBounds()
            .combinedClickable(onClick = { onPick(item.pick) },
                onLongClick = { if (item.url != null) preview.visible = true }),
        contentAlignment = Alignment.Center,
    ) {
        if (item.url == null) {
            Text(text = item.pick, fontSize = 16.sp, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        } else {
            AsyncImage(
                model = item.url, imageLoader = emoteImageLoader(),
                contentDescription = item.pick,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(layout.imageSize.coerceAtMost(layout.cell)),
            )
        }
    }
}

internal class EmojiPreviewStateHolder { var visible by mutableStateOf(false) }

@Composable
private fun EmojiPreview(item: EmojiCell, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer,
            RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(model = item.url, imageLoader = emoteImageLoader(), contentDescription = item.pick,
                modifier = Modifier.size(180.dp), contentScale = ContentScale.Fit)
            Text(item.pick, color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceContainer)
        }
    }
}
