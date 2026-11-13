package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
) {
    val colors = MiuixTheme.colorScheme

    Column(
        modifier = modifier
            .clip(FeedCardShape)
            .background(colors.surfaceContainer)
            .combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = onLongClick),
    ) {
        GridVideoCover(video, progress, selectionEnabled, selected, reserveMeta)
        GridVideoContent(video, showOwner)
    }
}
