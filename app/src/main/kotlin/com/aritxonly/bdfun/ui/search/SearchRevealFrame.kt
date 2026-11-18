package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchRevealFrame(
    submitted: String?, reveal: Float, modifier: Modifier, content: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val t = reveal.coerceIn(0f, 1f)
    val contentReveal = t

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val pillCenterY = with(density) { (statusTop + HomeTopBarHeight / 2).toPx() }
        val pillHeightPx = with(density) { 40.dp.toPx() }
        val pillWidthPx = widthPx * 0.80f

        // 动画结束后不再保留离屏图层，避免文字被当成半透明层渲染而显得变细。
        val revealLayer = if (t < 0.999f) {
            Modifier.graphicsLayer { alpha = t }
        } else {
            Modifier
        }
        val contentLayer = if (contentReveal < 0.999f) {
            Modifier.graphicsLayer {
                alpha = contentReveal
                translationY = (1f - contentReveal) * 22.dp.toPx()
            }
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(revealLayer)
                .background(colors.background),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (submitted == null) Modifier.statusBarsPadding() else Modifier)
                    // 结果页不再给整块内容留顶栏高度，让列表能滚到顶栏下面被模糊；
                    // 搜索首页（记录/热搜）仍从顶栏下方开始。
                    .padding(top = if (submitted == null) HomeTopBarHeight else 0.dp)
                    .then(contentLayer),
            ) {
                content()
            }
        }
    }
}
