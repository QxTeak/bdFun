package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 共享卡片保持原anchor与绘制；首页模块只传入打开/长按动作。 */
@Composable
internal fun HomeFeedCard(
    video: VideoItem,
    largeCards: Boolean,
    onOpen: () -> Unit,
    onLongClick: (Rect) -> Unit,
    onMoreClick: (Rect) -> Unit,
    moreMorphState: com.aritxonly.teak.common.navigation.TeakMorphMenuState? = null,
    onMoreMorphTrigger: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    val oneTake = rememberOneTakeAnchor()
    val radiusPx = with(density) { (if (largeCards) 14.dp else 16.dp).toPx() }
    val bgArgb = colors.background.toArgb()
    // 刚点“不感兴趣”的卡留在原位：封面重度模糊，卡内只保留一个撤销入口。
    val blurred = video.bvid in com.aritxonly.bdfun.feature.home.presentation.LocalHomeBlurredBvids.current
    val undo = com.aritxonly.bdfun.feature.home.presentation.LocalHomeUndoAction.current
    val undoMessage = if (blurred) {
        com.aritxonly.bdfun.feature.home.presentation.LocalHomeUndoMessages.current[video.bvid]
    } else null
    val undoThis = if (blurred) ({ undo(video.bvid) }) else null
    val openCard = {
        oneTake.open(
            context = context,
            video = video,
            radiusPx = radiusPx,
            backgroundColor = bgArgb,
        ) { onOpen() }
    }
    if (largeCards) {
        VideoLargeCard(video = video, modifier = oneTake.modifier,
            onClick = if (blurred) ({}) else openCard,
            onLongPressRect = if (blurred) null else onLongClick,
            onMoreClick = if (blurred) null else onMoreClick,
            moreMorphState = moreMorphState, onMoreMorphTrigger = onMoreMorphTrigger,
            undo = VideoCardUndo(blurred = blurred, undoOnly = blurred, onUndo = undoThis, undoMessage = undoMessage))
    } else {
        VideoGridCard(video = video, modifier = oneTake.modifier,
            onClick = if (blurred) ({}) else openCard,
            onLongPressRect = if (blurred) null else onLongClick,
            onMoreClick = if (blurred) null else onMoreClick,
            more = VideoCardMorph(moreMorphState, onMoreMorphTrigger),
            undo = VideoCardUndo(
                blurred = blurred, undoOnly = blurred, onUndo = undoThis, undoMessage = undoMessage,
            ))
    }
}

