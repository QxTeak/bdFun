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
) {
    val colors = MiuixTheme.colorScheme
    val bounds = remember { CardBoundsHolder() }

    Box(
        modifier = modifier.onGloballyPositioned { bounds.value = it.boundsInWindow() },
    ) {
        Column(
            modifier = Modifier
                .clip(FeedCardShape)
                .background(colors.surfaceContainer)
                .combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onClick,
                    onLongClick = {
                        when {
                            onLongPressRect != null -> onLongPressRect(bounds.value)
                            else -> onLongClick?.invoke()
                        }
                    },
                ),
        ) {
            GridVideoCover(video, progress, selectionEnabled, selected, reserveMeta)
            GridVideoContent(video, showOwner, onMoreClick = onMoreClick?.let { more -> { more(bounds.value) } })
        }
    }
}
