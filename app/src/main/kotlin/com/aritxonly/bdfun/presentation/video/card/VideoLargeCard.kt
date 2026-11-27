package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoLargeCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    onLongClick: (() -> Unit)? = null,
    onLongPressRect: ((Rect) -> Unit)? = null,
    onMoreClick: ((Rect) -> Unit)? = null,
    moreMorphState: com.aritxonly.teak.common.navigation.TeakMorphMenuState? = null,
    onMoreMorphTrigger: (() -> Unit)? = null,
    /** 「不感兴趣」撤销态：模糊、只留撤销入口、提示文案。 */
    undo: VideoCardUndo = VideoCardUndo(),
) {
    val colors = MiuixTheme.colorScheme
    val bounds = remember { CardBoundsHolder() }
    // 长按起点只在指针事件和长按回调里读写，用普通字段，避免滚动时写快照状态。
    val press = remember { CardPressPoint() }
    val localMorph = LocalVideoCardMorphMenu.current
    val morphState = moreMorphState ?: if (localMorph != null) {
        remember(video.bvid) { com.aritxonly.teak.common.navigation.TeakMorphMenuState() }
    } else null
    val morphTrigger = onMoreMorphTrigger ?: if (localMorph != null && morphState != null) {
        { localMorph.show(video, morphState, null) }
    } else null
    val pointState = if (localMorph != null && moreMorphState == null) {
        remember(video.bvid) { com.aritxonly.teak.common.navigation.TeakMorphMenuState() }
    } else null
    val longPressRect = onLongPressRect ?: if (localMorph != null && pointState != null) {
        { rect -> localMorph.show(video, pointState, androidx.compose.ui.geometry.Offset(rect.left, rect.top)) }
    } else null

    Box(
        modifier = modifier.fillMaxWidth().onGloballyPositioned { bounds.attach(it) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ContinuousCornerShape(14.dp))
                .background(colors.surfaceContainer)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            event.changes.firstOrNull { it.pressed }?.let { press.value = it.position }
                        }
                    }
                }
                .combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onClick,
                    onLongClick = {
                        when {
                            longPressRect != null -> run { val b = bounds.value; val p = androidx.compose.ui.geometry.Offset(b.left + press.value.x, b.top + press.value.y); longPressRect(androidx.compose.ui.geometry.Rect(p.x, p.y, p.x, p.y)) }
                            else -> onLongClick?.invoke()
                        }
                    },
                ),
        ) {
            LargeVideoCover(video, progress, blurred = undo.blurred, undoMessage = undo.undoMessage)
            LargeVideoContent(
                video,
                onMoreClick = onMoreClick?.let { more -> { more(bounds.value) } },
                moreMorphState = morphState,
                onMoreMorphTrigger = morphTrigger,
                undoOnly = undo.undoOnly,
                onUndo = undo.onUndo,
            )
        }
    }
}


