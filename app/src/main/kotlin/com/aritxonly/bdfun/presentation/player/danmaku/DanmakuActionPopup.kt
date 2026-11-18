package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

@Composable
internal fun DanmakuActionPopup(
    position: Offset, liked: Boolean, count: Long, onDismiss: () -> Unit,
    onLike: () -> Unit, onCopy: () -> Unit, onReport: (() -> Unit)?, onMore: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val positioner = remember(position) { DanmakuPopupPosition(position) }
    val likeLabel = if (liked) "取消点赞" else "点赞弹幕"
    Popup(positioner, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Row(Modifier.background(colors.surface, RoundedCornerShape(16.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLike, modifier = Modifier.semantics { contentDescription = "$likeLabel，$count 个赞" }) {
                val tint = if (liked) colors.primary else colors.onSurface
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppGlyph(if (liked) TeakIcon.Like else TeakIcon.LikeOutline, tint, size = 20.dp)
                    if (count > 0) Text(count.toString(), color = tint, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onCopy) { AppGlyph(MiuixIcons.Copy, colors.onSurface, size = 20.dp, contentDescription = "复制弹幕") }
            onReport?.let {
                IconButton(onClick = it) { AppGlyph(MiuixIcons.Report, colors.onSurface, size = 20.dp, contentDescription = "举报弹幕") }
            }
            IconButton(onClick = onMore) { AppGlyph(MiuixIcons.More, colors.onSurface, size = 20.dp, contentDescription = "更多弹幕操作") }
        }
    }
}

private class DanmakuPopupPosition(private val point: Offset) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection,
        popupContentSize: IntSize): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val below = point.y.roundToInt()
        val y = if (below <= maxY) below else below - popupContentSize.height
        return IntOffset((point.x.roundToInt() - popupContentSize.width / 2).coerceIn(0, maxX), y.coerceIn(0, maxY))
    }
}
