package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
                    filterOpen = filterOpen,
                    onFilterOpenChange = onFilterOpenChange,
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
            if (wideResultsBar != null) {
                com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial() {
                    Box(Modifier.statusBarsPadding()) { wideResultsBar { header(Modifier) } }
                }
            } else {
                // 独立窗口承载 segment：位置不变，但不在 layerBackdrop 的录制树里。
                androidx.compose.ui.window.Popup(
                    alignment = Alignment.TopCenter,
                    properties = androidx.compose.ui.window.PopupProperties(
                        focusable = false,
                        clippingEnabled = false,
                    ),
                ) {
                    // Popup 的内容用无界约束测量，必须显式给屏幕宽，fillMaxWidth 单独用会量成 0。
                    val popupWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
                    Box(Modifier.width(popupWidth)) {
                        Box(
                            Modifier
                                .width(popupWidth)
                                .statusBarsPadding()
                                .padding(top = HomeTopBarHeight + 6.dp),
                        ) { header(Modifier) }
                    }
                }
            }
        }
    }
}
