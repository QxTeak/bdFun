package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.aritxonly.bdfun.core.domain.SearchFilterState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import kotlin.math.roundToInt

@Composable
internal fun SearchResults(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState, ui: SearchPresentationStateHolder,
    onOpenVideo: (VideoItem) -> Unit, onOpenUser: (SearchUser) -> Unit,
    onLoadMore: () -> Unit = {},
    onLoadMoreUsers: () -> Unit = {},
    wideResultsBar: (@Composable (@Composable () -> Unit) -> Unit)? = null,
) {
    val tab = ui.tab
    val filterOpen = ui.filterOpen
    val filters = ui.filters
    val onTabChange: (Int) -> Unit = { ui.tab = it }
    val onFilterOpenChange: (Boolean) -> Unit = { ui.filterOpen = it }
    val onFiltersChange: (SearchFilterState) -> Unit = { ui.filters = it }
    // 应用筛选要写回 appliedFilters：搜索 effect 只观察它，写 filters 不会触发重新搜索。
    val onApplyFilters: (SearchFilterState) -> Unit = { ui.appliedFilters = it }
    val presentation = rememberSearchResultsPresentation(tab, onTabChange)
    // 菜单状态提升到页面：header（anchor）和形变宿主必须挂在同一个窗口里，而窗口按宽窄布局不同。
    val filterMenu = rememberSearchFilterMenuController(filterOpen, onFilterOpenChange)
    val filterMenuVisible = filterOpen || filterMenu.state.isOpen || filterMenu.keepAlive
    // Tab 栏跟随手指方向收起/露出：往上滑隐藏，往下滑重新出现，减少对内容的遮挡。
    val pageState = com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current
    val tabsAway by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (wideResultsBar == null) pageState.scrollAway else 0f,
        animationSpec = androidx.compose.animation.core.tween(220),
        label = "searchTabsAway",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(presentation.backdrop),
        ) {
            SearchResultPager(presentation, content, onOpenVideo, onOpenUser, onLoadMore, onLoadMoreUsers)
        }

        // Tab / 筛选按钮固定不动，只跟随页面本身的滚动，不再随列表上移。
        CompositionLocalProvider(LocalTeakBackdrop provides presentation.backdrop) {
            val header: @Composable (Modifier) -> Unit = { extra ->
                ResultsTabHeader(
                    tab = tab,
                    onTabChange = onTabChange,
                    position = presentation.position,
                    filterMenu = filterMenu,
                    filters = filters,
                    onFiltersChange = onFiltersChange,
                    onApplyFilters = onApplyFilters,
                    modifier = extra.fillMaxWidth().graphicsLayer {
                        // 恢复原来“滚上去”的运动，但整条画在独立 Popup 里，
                        // 不进入顶栏 progressive blur 的录制层，因此不会被模糊。
                        translationY = -48.dp.toPx() * tabsAway
                        alpha = 1f - tabsAway
                    },
                )
            }
            SearchResultsChrome(
                wideResultsBar = wideResultsBar,
                header = header,
                filterMenu = filterMenu,
                filterMenuVisible = filterMenuVisible,
                filters = filters,
                onFiltersChange = onFiltersChange,
                onApplyFilters = onApplyFilters,
            )
        }
    }
}

/**
 * 搜索结果页顶栏与筛选形变菜单宿主。
 *
 * 宿主必须和 anchor 在同一个窗口坐标系里：窄屏 header 画在独立 Popup 中（避免进入顶栏
 * progressive blur 录制层），宿主就摆在同一 Popup 的整屏区域内；宽屏 header 在主窗口顶栏里，
 * 宿主放页面根部（和收藏页 menuOverlay 同层），同一窗口且画在顶栏之上。
 */
@Composable
private fun SearchResultsChrome(
    wideResultsBar: (@Composable (@Composable () -> Unit) -> Unit)?,
    header: @Composable (Modifier) -> Unit,
    filterMenu: SearchFilterMenuController,
    filterMenuVisible: Boolean,
    filters: SearchFilterState,
    onFiltersChange: (SearchFilterState) -> Unit,
    onApplyFilters: (SearchFilterState) -> Unit,
) {
    if (wideResultsBar != null) {
        com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial() {
            Box(Modifier.statusBarsPadding()) { wideResultsBar { header(Modifier) } }
        }
        SearchFilterMorphHost(
            visible = filterMenuVisible,
            state = filterMenu.state,
            onDismiss = filterMenu::requestClose,
            filters = filters,
            onFiltersChange = onFiltersChange,
            onApplyFilters = onApplyFilters,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    // 独立窗口承载 segment：位置不变，但不在 layerBackdrop 的录制树里。
    // Popup 本身从顶栏下沿开始，而不是盖住整条顶栏：Popup 窗口会吞掉范围内所有触摸，
    // 若从 y=0 开始，结果页上方的返回键和搜索框就点不动了。
    val density = androidx.compose.ui.platform.LocalDensity.current
    val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBars
        .asPaddingValues().calculateTopPadding()
    val popupTop = statusTop + HomeTopBarHeight + 6.dp
    val popupTopPx = with(density) { popupTop.roundToPx() }
    androidx.compose.ui.window.Popup(
        alignment = Alignment.TopCenter,
        offset = androidx.compose.ui.unit.IntOffset(0, popupTopPx),
        properties = androidx.compose.ui.window.PopupProperties(
            focusable = false,
            clippingEnabled = false,
        ),
    ) {
        // Popup 的内容用无界约束测量，必须显式给屏幕尺寸，fillMaxSize 单独用会量成 0。
        val popupWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        val popupHeight = (screenHeight - popupTop).coerceAtLeast(240.dp)
        Box(Modifier.width(popupWidth)) {
            Box(Modifier.width(popupWidth)) { header(Modifier) }
            // 菜单打开时把宿主撑到屏幕剩余高度：宿主、点击收起层都和 anchor 同窗口，
            // 面板不会被同窗口的 Tab 栏压住；收起动画结束后宿主卸载，窗口恢复成 header 大小。
            SearchFilterMorphHost(
                visible = filterMenuVisible,
                state = filterMenu.state,
                onDismiss = filterMenu::requestClose,
                filters = filters,
                onFiltersChange = onFiltersChange,
                onApplyFilters = onApplyFilters,
                modifier = Modifier.width(popupWidth).height(popupHeight),
            )
        }
    }
}
