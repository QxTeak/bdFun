package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LargeVideoContent(
    video: VideoItem,
    onMoreClick: (() -> Unit)? = null,
    moreMorphState: com.aritxonly.teak.common.navigation.TeakMorphMenuState? = null,
    onMoreMorphTrigger: (() -> Unit)? = null,
    undoOnly: Boolean = false,
    onUndo: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val undoAlpha = animateFloatAsState(
        targetValue = if (undoOnly) 1f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "large_undo_fade",
    )

    Column(modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 9.dp, bottom = 9.dp)) {
        Box(Modifier.fillMaxWidth()) {
            // 撤销态保留整块正常文字（透明）占位，卡片高度和正常态完全一致。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 1f - undoAlpha.value }
                    // 透明占位不参与无障碍语义，读屏只会读到上面的“撤销”。
                    .then(if (undoOnly) Modifier.clearAndSetSemantics {} else Modifier),
            ) {
                Text(
                    text = video.title,
                    color = colors.onBackground,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 38.dp),
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UpBadge()
                    if (video.isFollowed) {
                        Spacer(Modifier.width(4.dp))
                        FollowedBadge()
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = video.ownerName,
                        color = colors.onBackgroundVariant,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(interactionSource = null, indication = null) {
                                navigateToSpace(context, video.ownerMid)
                            },
                    )
                    if (onMoreClick != null || moreMorphState != null) {
                        VideoCardMoreButton(
                            onClick = onMoreClick ?: {},
                            touchSize = 22.dp,
                            iconSize = 15.dp,
                            morphState = moreMorphState,
                            onMorphTrigger = onMoreMorphTrigger,
                        )
                    }
                }
            }
            // 撤销层始终保留并在 0/1 之间淡入淡出；只有撤销态才接管点击。
            LargeVideoUndoLayer(undoAlpha.value, undoOnly, onUndo, colors)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.LargeVideoUndoLayer(
    alpha: Float,
    undoOnly: Boolean,
    onUndo: (() -> Unit)?,
    colors: top.yukonga.miuix.kmp.theme.Colors,
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (undoOnly) {
                    Modifier.clickable(interactionSource = null, indication = null) { onUndo?.invoke() }
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppGlyph(MiuixIcons.Undo, tint = colors.primary, size = 16.dp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "撤销",
                color = colors.primary,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = 1,
            )
        }
    }
}
