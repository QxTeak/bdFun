package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.domain.SearchFilterState
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import kotlinx.coroutines.delay

/**
 * 收起后仍保留宿主的时间，够 Miuix 的回落弹簧落回按钮。
 *
 * 窄屏宿主在 Popup 里，窗口要等这段时间结束才缩回 header 大小，期间会挡住页面触摸，
 * 所以压到回落动画的主体时长，不跟 TeakMorphMenuRegistry 的 1200ms 对齐。
 */
private const val FilterMenuExitKeepMillis = 420L

/**
 * 搜索结果页筛选形变菜单的会话状态。
 *
 * 漏斗 anchor 在 header 里，而 header 的位置随窗口不同：窄屏整条 header 画在独立 Popup 里，
 * 宽屏画在主窗口顶栏里。所以菜单状态提升到这里，由页面在「和 anchor 同一个窗口」的位置
 * 挂 [SearchFilterMorphHost]，两边共用同一个 [TeakMorphMenuState]。
 */
@Stable
internal class SearchFilterMenuController internal constructor(
    internal val state: TeakMorphMenuState,
    private val onOpenChange: (Boolean) -> Unit,
) {
    /** 回落动画期间宿主还要留在组合里；页面据此决定是否继续挂宿主。 */
    internal var keepAlive by mutableStateOf(false)

    /** 点漏斗：只把外部开关置 true，真正打开由 [rememberSearchFilterMenuController] 的 effect 完成。 */
    internal fun requestOpen() = onOpenChange(true)

    /** 菜单自己收起（点面板外 / 返回键）时回写外部开关。 */
    internal fun requestClose() = onOpenChange(false)
}

/**
 * 把外部 `filterOpen` 与 [TeakMorphMenuState] 双向同步，并维护收起动画的宿主存活期。
 *
 * 外部开关仍是唯一事实来源：点漏斗只写外部状态，再由 effect 打开菜单；菜单内部收起时
 * 只认真正的 open `true -> false`，所以开场前不会把开关误写回 false、也不会重复打开。
 */
@Composable
internal fun rememberSearchFilterMenuController(
    filterOpen: Boolean,
    onFilterOpenChange: (Boolean) -> Unit,
): SearchFilterMenuController {
    val state = remember { TeakMorphMenuState() }
    val currentOnChange by rememberUpdatedState(onFilterOpenChange)
    val controller = remember(state) {
        SearchFilterMenuController(state) { currentOnChange(it) }
    }

    // 外部开关 -> 菜单。先等两帧让宿主完成组合与测量，形变才有正确的起点矩形，
    // 否则第一次点开会直接出现在全展开位置（沿用 TeakMorphMenuAnchor 的开场约定）。
    LaunchedEffect(filterOpen) {
        if (!filterOpen) {
            state.close()
            return@LaunchedEffect
        }
        if (!state.isOpen) {
            withFrameNanos { }
            withFrameNanos { }
            state.openWithAnchorCopy(true)
        }
    }
    // 菜单 -> 外部开关：只认真正的 open true -> false，避免开场前就把开关写回 false。
    LaunchedEffect(state) {
        var wasOpen = state.isOpen
        snapshotFlow { state.isOpen }.collect { open ->
            if (wasOpen && !open) currentOnChange(false)
            wasOpen = open
        }
    }
    // 收起动画播完前保留宿主，否则回落弹簧会被提前拆掉、按钮会闪一下。
    LaunchedEffect(filterOpen, state.isOpen) {
        if (filterOpen || state.isOpen) {
            controller.keepAlive = true
        } else if (controller.keepAlive) {
            delay(FilterMenuExitKeepMillis)
            if (!state.isOpen) controller.keepAlive = false
        }
    }
    return controller
}

/**
 * 玻璃形变菜单宿主与筛选内容。
 *
 * [modifier] 由调用方给出「整屏」尺寸并把它摆到和 anchor 相同的窗口里：
 * 窄屏是 header 所在的那个 Popup 的整屏区域，宽屏是页面根部（和收藏页 menuOverlay 同层）。
 * 宿主只在打开中 / 回落中组合，收起后立刻交还触摸。
 */
@Composable
internal fun SearchFilterMorphHost(
    visible: Boolean,
    state: TeakMorphMenuState,
    onDismiss: () -> Unit,
    filters: SearchFilterState,
    onFiltersChange: (SearchFilterState) -> Unit,
    onApplyFilters: (SearchFilterState) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    TeakMorphMenuHost(
        state = state,
        modifier = modifier,
        minWidth = 280.dp,
        maxWidth = 340.dp,
        maxHeight = 520.dp,
        onDismiss = onDismiss,
    ) { _ ->
        FilterMenu(
            value = filters,
            onChange = {
                // 写回时序保持原样：先更新筛选草稿，再触发实际搜索。
                onFiltersChange(it)
                onApplyFilters(it)
            },
        )
    }
}
