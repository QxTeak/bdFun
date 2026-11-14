package com.aritxonly.bdfun.ui


import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import kotlinx.coroutines.launch

@Composable
fun androidx.compose.foundation.layout.BoxScope.VideoPageTabs(
    layout: VideoActivityLayout, playerBottomPx: () -> Float,
    pager: androidx.compose.foundation.pager.PagerState, onSelect: (Int) -> Unit,
    danmakuOn: Boolean, onOpenInput: () -> Unit, onToggleDanmaku: () -> Unit,
) {
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    val scrollProgress = com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current.materialProgress
    val materialProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = scrollProgress,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 280, easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "video-top-controls-material",
    )
    androidx.compose.runtime.CompositionLocalProvider(
        com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress provides scrollProgress,
        com.aritxonly.teak.common.chrome.LocalProgressiveTopBarMaterialHosted provides true,
        com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress provides materialProgress,
    ) {
        // 横屏保留顶部渐进模糊；竖屏仅控件渐显玻璃，不覆盖简介内容。
        com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial(
            enabled = splitPanes,
            modifier = Modifier
                .align(if (splitPanes) Alignment.TopEnd else Alignment.TopStart)
                .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier.fillMaxWidth())
                .graphicsLayer {
                    alpha = pageOverlayAlpha
                    translationY = if (splitPanes) 0f else playerBottomPx()
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = if (splitPanes) statusBarDp else 0.dp)
                    .height(52.dp)
                    .padding(horizontal = if (splitPanes) 8.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeakPagerTabs(
                    options = listOf("简介", "评论"), selected = pager.currentPage,
                    onSelect = onSelect,
                    itemWidth = if (splitPanes) 48.dp else 60.dp,
                    flatBackgroundColor = com.aritxonly.teak.common.theme.LocalTeakContentColors.current.groupedBackground,
                )
                VideoDanmakuActions(splitPanes, danmakuOn, onOpenInput, onToggleDanmaku)
            }
        }
    }

}

@Composable
private fun RowScope.VideoDanmakuActions(
    compact: Boolean, danmakuOn: Boolean, onOpenInput: () -> Unit, onToggleDanmaku: () -> Unit,
) {
    Spacer(Modifier.weight(1f))
    VideoDanmakuControl(compact, danmakuOn, onOpenInput, onToggleDanmaku)
}
