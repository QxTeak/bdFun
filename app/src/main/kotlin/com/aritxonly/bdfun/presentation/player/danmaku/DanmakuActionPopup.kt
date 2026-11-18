package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.domain.danmakuTimestampMs
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.settings.LocalTeakDropdownInteractive
import com.aritxonly.teak.common.settings.TeakDropdownPanel
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.icon.extended.Send
import kotlin.math.roundToInt

@Composable
internal fun DanmakuActionPopup(
    position: Offset, liked: Boolean, count: Long, item: DanmakuItem, onDismiss: () -> Unit,
    onLike: () -> Unit, onCopy: () -> Unit, onReport: (() -> Unit)?, onMore: (PoolAction) -> Unit,
) {
    val positioner = remember(position) { DanmakuPopupPosition(position) }
    val likeLabel = if (liked) "取消点赞" else "点赞弹幕"
    val host = LocalView.current
    var expanded by remember(item.id, position) { mutableStateOf(false) }
    var moreAnchor by remember(position) { mutableStateOf<Rect?>(null) }
    Popup(positioner, onDismissRequest = { if (!expanded) onDismiss() },
        properties = PopupProperties(focusable = true)) {
        Row(
            Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.68f)).padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DanmakuCapsuleButton("$likeLabel，$count 个赞", onLike) {
                val tint = if (liked) BrandPink else Color.White
                AppGlyph(TeakIcon.like(liked), tint, size = 15.dp)
                if (count > 0) Text(count.toString(), color = tint, fontSize = 10.sp)
            }
            DanmakuCapsuleButton("复制弹幕", onCopy) { AppGlyph(MiuixIcons.Copy, Color.White, size = 15.dp) }
            onReport?.let {
                DanmakuCapsuleButton("举报弹幕", it) { AppGlyph(MiuixIcons.Report, Color.White, size = 15.dp) }
            }
            DanmakuCapsuleButton("更多弹幕操作", { if (moreAnchor != null) expanded = true },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val screen = coordinates.positionOnScreen()
                    if (screen.isSpecified) {
                        val location = IntArray(2)
                        host.rootView.getLocationOnScreen(location)
                        val origin = Offset(location[0].toFloat(), location[1].toFloat())
                        moreAnchor = Rect(screen - origin, Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()))
                    }
                }) {
                AppGlyph(MiuixIcons.More, Color.White, size = 15.dp)
            }
        }
    }
    DanmakuMoreDropdown(item, expanded, moreAnchor, onDismiss = { expanded = false }, onMore)
}

@Composable
private fun DanmakuCapsuleButton(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.widthIn(min = 30.dp).height(30.dp)
            .semantics { contentDescription = label }.clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun DanmakuMoreDropdown(
    item: DanmakuItem, visible: Boolean, anchor: Rect?, onDismiss: () -> Unit, onSelect: (PoolAction) -> Unit,
) {
    val actions = remember(item) {
        buildList {
            add(PoolAction.Seek)
            if (danmakuTimestampMs(item.text, Long.MAX_VALUE) != null) add(PoolAction.Timestamp)
            add(PoolAction.FollowSend)
            if (item.self) add(PoolAction.Recall)
            else if (item.userHash.isNotBlank()) add(PoolAction.Block)
        }
    }
    TeakDropdownPanel(visible, onDismiss, width = 170.dp, anchorBounds = anchor, anchorTopGap = 4.dp, anchorEndGap = 0.dp) {
        actions.forEach { action ->
            val icon = when (action) {
                PoolAction.Seek -> MiuixIcons.Play
                PoolAction.Timestamp -> MiuixIcons.Link
                PoolAction.Recall -> MiuixIcons.Undo
                PoolAction.Block -> MiuixIcons.Blocklist
                PoolAction.FollowSend -> MiuixIcons.Send
                else -> null
            }
            GlassPopupItem(text = action.title, enabled = LocalTeakDropdownInteractive.current, icon = icon,
                onClick = { onDismiss(); onSelect(action) })
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
