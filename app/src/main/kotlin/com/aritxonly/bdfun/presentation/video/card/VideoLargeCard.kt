package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
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
) {
    val colors = MiuixTheme.colorScheme
    val bounds = remember { CardBoundsHolder() }

    Box(
        modifier = modifier.fillMaxWidth().onGloballyPositioned { bounds.value = it.boundsInWindow() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ContinuousCornerShape(14.dp))
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
            LargeVideoCover(video, progress)
            LargeVideoContent(
                video,
                onMoreClick = onMoreClick?.let { more -> { more(bounds.value) } },
                moreMorphState = moreMorphState,
                onMoreMorphTrigger = onMoreMorphTrigger,
            )
        }
    }
}
