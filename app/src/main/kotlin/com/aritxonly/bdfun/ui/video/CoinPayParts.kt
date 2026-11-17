package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward

/** 投币页的硬币箱：点击即选中这一档。 */
@Composable
internal fun CoinSlot(
    index: Int, selected: Boolean, progress: Float, paying: Boolean,
    onClick: () -> Unit,
) {
    // 选中：箱 100dp / 币 50dp；未选中 70dp / 35dp（上游 factor 0..1 的线性插值）。
    val boxSize = if (selected) 100.dp else 70.dp
    val coinSize = if (selected) 50.dp else 35.dp
    Box(
        modifier = Modifier
            .size(boxSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !paying,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.teak_coin_box),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        // 两页都画硬币，只是未选中的小一号（上游 factor 0..1 的尺寸插值）。
        Image(
            painter = painterResource(if (index == 0) R.drawable.teak_coins_one else R.drawable.teak_coins_two),
            contentDescription = null,
            modifier = Modifier
                .size(coinSize)
                .graphicsLayer {
                    if (!paying || !selected) return@graphicsLayer
                    // 硬币上抛 2 倍高度，最后 1/3 淡出。
                    val throwProgress = ((progress - 0.15f) / 0.5f).coerceIn(0f, 1f)
                    translationY = -throwProgress * 2f * size.height
                    alpha = (1f - ((progress - 0.65f) / 0.35f).coerceIn(0f, 1f))
                },
        )
    }
}

@Composable
internal fun CoinArrow(
    forward: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Icon(
        imageVector = if (forward) MiuixIcons.ChevronForward else MiuixIcons.ChevronBackward,
        contentDescription = if (forward) "下一档" else "上一档",
        tint = Color.White.copy(alpha = if (enabled) 0.92f else 0.30f),
        modifier = modifier
            .size(28.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    )
}
