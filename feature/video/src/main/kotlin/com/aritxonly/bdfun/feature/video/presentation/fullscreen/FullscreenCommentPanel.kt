package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.rememberManualProgress

/**
 * 全屏评论面板（横屏）。
 *
 * 比其它二级面板宽：评论列表需要可读的正文宽度，右栏太窄会把长评论压成一条细柱。
 * 交互与视觉沿用 [FullscreenPanelHost] 的「左透明右渐深」：点画面任意处收起。
 */
@Composable
internal fun FullscreenCommentPanel(
    info: FullscreenPlayerInfo,
    onClose: () -> Unit,
    visible: Boolean,
    comments: @Composable (FullscreenPlayerInfo, androidx.compose.ui.unit.Dp, Modifier) -> Unit,
    composer: @Composable (Modifier) -> Unit,
) {
    var revealed by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) { revealed = visible }
    val reveal = rememberManualProgress(if (revealed) 1f else 0f, 264f)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 横屏宽度普遍在 780dp 以上，0.44 约 340dp 起步，最窄也留 400dp 给正文。
        val panelWidth = (maxWidth.value * 0.44f).dp.coerceAtLeast(400.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.28f to Color.Transparent,
                        0.48f to Color.Black.copy(alpha = 0.25f),
                        0.62f to Color.Black.copy(alpha = 0.74f),
                        0.72f to Color.Black.copy(alpha = 0.95f),
                        1f to Color.Black,
                    ),
                )
                .graphicsLayer { alpha = reveal }
                .clickable(interactionSource = null, indication = null, onClick = onClose),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(panelWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = reveal
                    translationX = (1f - reveal) * 90f
                }
                // 空白处的点击不能穿透到底下的画面，否则点列表边缘会顺手收起面板。
                .clickable(interactionSource = null, indication = null) {}
                .padding(start = 20.dp, end = 22.dp, top = 24.dp),
        ) {
            // 这块面板画在播放器之上的 z 轴覆层里，而活动根部的 LayerBackdrop 会把这层覆层一起录进底片。
            // 玻璃控件若在这里继续采样那份 backdrop，就形成「底片包含采样方」的自引用环，
            // RenderThread 的 prepareTreeImpl 会一路递归到栈溢出直接 SIGSEGV（表现为应用闪退回启动页）。
            // 置空后玻璃退化为平面底色，视觉上仍是深色面板，行为正确。
            androidx.compose.runtime.CompositionLocalProvider(
                com.aritxonly.teak.common.material.LocalTeakBackdrop provides null,
            ) {
                Box(modifier = Modifier.weight(1f)) { comments(info, panelWidth, Modifier.fillMaxSize()) }
                composer(Modifier)
            }
        }
    }
}