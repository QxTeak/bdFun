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
    val pageScroll = com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current
    // 只在“是否滚出顶部”翻转时重组；不能每滚一像素都读一次进度，否则整排 Tab
    // 会跟着滚动逐帧重组，玻璃出现的那 300ms 更容易掉帧。
    val materialTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.derivedStateOf { pageScroll.materialProgress > 0f }
    }
    // 顶栏材质统一走「固定 300ms 补间」：和收藏页返回键同一套 rememberManualProgress，
    // 不随滚动速度忽快忽慢；滚出顶部才起跑，回到顶部再同样速度淡出。
    val materialReveal = com.aritxonly.teak.common.chrome.rememberManualProgress(
        if (materialTarget) 1f else 0f,
        300f,
    )
    // 视频页没有 ProgressiveTopBarMaterial，这个 local 只为兼容读取方；给阈值而不是
    // 连续进度，避免每滚一像素都让整棵子树重组。
    val scrollProgress = if (materialTarget) 1f else 0f
    val tabsModifier = Modifier
        .align(if (splitPanes) Alignment.TopEnd else Alignment.TopStart)
        .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier.fillMaxWidth())
        .graphicsLayer {
            alpha = pageOverlayAlpha
            translationY = if (splitPanes) 0f else playerBottomPx()
        }
    val tabsRow: @Composable () -> Unit = {
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
    androidx.compose.runtime.CompositionLocalProvider(
        com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress provides scrollProgress,
        com.aritxonly.teak.common.chrome.LocalProgressiveTopBarMaterialHosted provides true,
        com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress provides materialReveal,
    ) {
        // 不做顶部渐进式模糊（横屏分栏也不要）：评论区顶部用 VideoPageBody 里的
        // 渐变遮罩，和底部保持对称；这里只负责 tab / 发弹幕控件的材质进度。
        androidx.compose.foundation.layout.Box(tabsModifier) { tabsRow() }
    }
}

@Composable
private fun RowScope.VideoDanmakuActions(
    compact: Boolean, danmakuOn: Boolean, onOpenInput: () -> Unit, onToggleDanmaku: () -> Unit,
) {
    Spacer(Modifier.weight(1f))
    VideoDanmakuControl(compact, danmakuOn, onOpenInput, onToggleDanmaku)
}
