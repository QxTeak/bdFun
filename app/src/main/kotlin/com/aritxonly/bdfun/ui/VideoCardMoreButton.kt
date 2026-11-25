package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.TeakMorphMenuAnchor
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 视频卡片右下角的省略号，和评论区行尾的操作按钮完全一致：
 * 同一个 Miuix `More` 图标、24dp 点击区、无涟漪。
 *
 * 传入 [morphState] 时使用 Miuix 玻璃形变菜单（和收藏页「更多」一致）：
 * 图标参与展开/收起动画；不传则保持普通点击回调（长按菜单走矩形锚点）。
 */
@Composable
internal fun VideoCardMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCover: Boolean = false,
    touchSize: Dp = 24.dp,
    iconSize: Dp = 16.dp,
    morphState: TeakMorphMenuState? = null,
    onMorphTrigger: (() -> Unit)? = null,
) {
    if (morphState != null) {
        TeakMorphMenuAnchor(
            state = morphState,
            modifier = modifier,
            cornerRadius = touchSize / 2,
            onClick = onMorphTrigger,
        ) { open ->
            MoreButtonContent(onCover, touchSize, iconSize, open)
        }
        return
    }
    MoreButtonContent(onCover, touchSize, iconSize, onClick, modifier)
}

@Composable
private fun MoreButtonContent(
    onCover: Boolean,
    touchSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .size(touchSize)
            .clip(CircleShape)
            .then(
                if (onCover) {
                    Modifier.background(Color.Black.copy(alpha = 0.38f))
                } else {
                    Modifier
                },
            )
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = MiuixIcons.More,
            contentDescription = "更多",
            tint = if (onCover) Color.White else colors.onBackgroundVariant,
            modifier = Modifier.size(iconSize),
        )
    }
}
