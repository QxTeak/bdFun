package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoGridCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 已看进度 0..1（历史记录用），默认不显示 */
    progress: Float = 0f,
    /** 批量管理：显示右上角勾选圈，点击仍交给 onClick 处理 */
    selectionEnabled: Boolean = false,
    selected: Boolean = false,
    /** 历史页：统计行先占位，避免补数据时整行突然出现 */
    reserveMeta: Boolean = false,
    /** 个人主页等场景不需要最下面的 UP 行。 */
    showOwner: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    /** 长按/省略号共用：传入卡片窗口坐标，下拉菜单据此定位。 */
    onLongPressRect: ((Rect) -> Unit)? = null,
    onMoreClick: ((Rect) -> Unit)? = null,
    more: VideoCardMorph? = null,
) {
    val colors = MiuixTheme.colorScheme
    val bounds = remember { CardBoundsHolder() }
    val press = remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    // 页面没有显式接线时使用本地形变菜单（空间等直接复用这张卡片的页面）。
    val localMorph = LocalVideoCardMorphMenu.current
    val moreMorphState = more?.state
    val morphState = moreMorphState ?: if (localMorph != null) {
        remember(video.bvid) { com.aritxonly.teak.common.navigation.TeakMorphMenuState() }
    } else null
    val morphTrigger = more?.onTrigger ?: if (localMorph != null && morphState != null) {
        { localMorph.show(video, morphState, null) }
    } else null
    val pointState = if (localMorph != null && moreMorphState == null) {
        remember(video.bvid) { com.aritxonly.teak.common.navigation.TeakMorphMenuState() }
    } else null
    val longPressRect = onLongPressRect ?: if (localMorph != null && pointState != null) {
        { rect -> localMorph.show(video, pointState, androidx.compose.ui.geometry.Offset(rect.left, rect.top)) }
    } else null

    Box(
        modifier = modifier.onGloballyPositioned { bounds.value = it.boundsInWindow() },
    ) {
        Column(
            modifier = Modifier
                .clip(FeedCardShape)
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
            GridVideoCover(video, progress, selectionEnabled, selected, reserveMeta)
            GridVideoContent(
                video, showOwner,
                onMoreClick = onMoreClick?.let { more -> { more(bounds.value) } },
                moreMorphState = morphState,
                onMoreMorphTrigger = morphTrigger,
            )
        }
    }
}
