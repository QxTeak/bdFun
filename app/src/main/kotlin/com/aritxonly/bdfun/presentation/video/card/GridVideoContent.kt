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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GridVideoContent(
    video: VideoItem,
    showOwner: Boolean,
    onMoreClick: (() -> Unit)? = null,
    moreMorphState: com.aritxonly.teak.common.navigation.TeakMorphMenuState? = null,
    onMoreMorphTrigger: (() -> Unit)? = null,
    undoOnly: Boolean = false,
    onUndo: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    val undoAlpha = animateFloatAsState(
        targetValue = if (undoOnly) 1f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "grid_undo_fade",
    )

    Column(modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 7.dp, bottom = 8.dp)) {
        Box(Modifier.fillMaxWidth()) {
            // 撤销态保留整块正常文字（透明）占位，卡片高度和正常态逐像素一致，
            // 不会因为 UP 行被替换成固定高度而缩卡。
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    modifier = Modifier.heightIn(min = TitleBlockHeight),
                )

                if (showOwner) {
                    Spacer(Modifier.height(5.dp))
                    GridVideoOwnerRow(video, onMoreClick, moreMorphState, onMoreMorphTrigger)
                }
            }
            // 撤销层始终保留并在 0/1 之间淡入淡出；只有撤销态才接管点击。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = undoAlpha.value }
                    .then(
                        if (undoOnly) {
                            Modifier.clickable(interactionSource = null, indication = null) { onUndo?.invoke() }
                        } else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                        AppGlyph(MiuixIcons.Undo, tint = colors.primary, size = 15.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "撤销",
                            color = colors.primary,
                            maxLines = 1,
                            style = TextStyle(
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                        )
                }
            }
        }
    }
}

@Composable
private fun GridVideoOwnerRow(
    video: VideoItem,
    onMoreClick: (() -> Unit)?,
    moreMorphState: com.aritxonly.teak.common.navigation.TeakMorphMenuState?,
    onMoreMorphTrigger: (() -> Unit)?,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 外层占满剩余宽度，保证省略号始终贴最右；只有内层徽标+名字可点。
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(Capsule())
                    .clickable(interactionSource = null, indication = null) {
                        navigateToSpace(context, video.ownerMid)
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UpBadge()
                if (video.isFollowed) {
                    Spacer(Modifier.width(4.dp))
                    FollowedBadge()
                }
                Spacer(Modifier.width(5.dp))
                Text(
                    text = video.ownerName,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onMoreClick != null || moreMorphState != null) {
            VideoCardMoreButton(
                onClick = onMoreClick ?: {},
                touchSize = 20.dp,
                iconSize = 14.dp,
                morphState = moreMorphState,
                onMorphTrigger = onMoreMorphTrigger,
            )
        }
    }
}
