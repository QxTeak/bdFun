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
    commentCount: Long = 0L,
) {
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    // 「评论 406」比「简介」宽，两格等宽后按分栏/竖屏分别加宽；读取 0 时退回纯「评论」，
    // 既不会闪一下「评论 0」，也不会在数据到达时让标签宽度跳变（字号与宽度都按最宽状态定）。
    val tabs = androidx.compose.runtime.remember(commentCount) {
        listOf("简介", videoCommentTabLabel(commentCount))
    }
    // 78/68 是按「评论 9999.9万」这类最坏文案量出来的：单格 80dp 留一点余量，
    // 分栏时详情栏最窄 360dp，70dp 两格加弹幕控件仍能排下。
    val tabItemWidth = if (splitPanes) 70.dp else 80.dp
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
    // local 提供 State 而不是 Float：tab 的 alpha 在绘制阶段读取，滚动时不重组整条子树。
    val revealState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(materialReveal) }
    androidx.compose.runtime.SideEffect { revealState.value = materialReveal }
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
                options = tabs, selected = pager.currentPage,
                onSelect = onSelect,
                itemWidth = tabItemWidth,
                flatBackgroundColor = com.aritxonly.teak.common.theme.LocalTeakContentColors.current.groupedBackground,
            )
            VideoDanmakuActions(splitPanes, danmakuOn, onOpenInput, onToggleDanmaku)
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(
        com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress provides scrollProgress,
        com.aritxonly.teak.common.chrome.LocalProgressiveTopBarMaterialHosted provides true,
        com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress provides revealState,
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

/**
 * Tab 上的评论标签文案。
 *
 * 数字口径跟播放量/弹幕数一致走 [com.aritxonly.bdfun.core.common.formatCount]（≥1万转「万/亿」），
 * 但去掉单位前的空格和没意义的 `.0`：Tab 每格只有 80dp，13sp 的「1.2 万」容易折行，
 * 「10.0万」也不如「10万」干净。
 * [count] 为 0 或负数（详情未加载、接口没给 stat.reply）时只出「评论」，
 * 避免先闪一个「评论 0」再跳成真实值。
 */
fun videoCommentTabLabel(count: Long): String {
    if (count <= 0L) return "评论"
    val compact = com.aritxonly.bdfun.core.common.formatCount(count)
        .replace(" ", "")
        .replace(".0万", "万")
        .replace(".0亿", "亿")
    return "评论 $compact"
}
