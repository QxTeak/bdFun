package com.aritxonly.teak.common.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.glass.GlassPopupItem

/** 一条玻璃形变菜单项（与具体业务无关）。 */
data class TeakMorphMenuItem(
    val text: String,
    val enabled: Boolean = true,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

/** 收起弹簧要等它真正落回锚点后再卸载；提前拆宿主会让超出回弹瞬间卡回原位。 */
private const val OverlayReleaseDelayMillis = 1200L

/**
 * 全 Activity 共用的形变菜单控制器：任意页面的省略号/长按只需要自己的
 * [TeakMorphMenuState]（每行一个），菜单内容和宿主统一由 Activity 根部渲染。
 *
 * 宿主必须挂在主壳玻璃采样树之外，否则会 RenderNode 自采样递归闪退。
 */
@Stable
class TeakMorphMenuController {
    var state by mutableStateOf<TeakMorphMenuState?>(null)
        private set
    var items by mutableStateOf<List<TeakMorphMenuItem>>(emptyList())
        private set
    var onDismiss by mutableStateOf<(() -> Unit)?>(null)
        private set

    /** 长按时的锚点（手指位置）；null 表示从按钮图标展开。 */
    var point by mutableStateOf<Offset?>(null)
        private set
    var showAnchorCopy by mutableStateOf(true)
        private set

    /** 每次打开加一：同一个按钮复用 state 时也要重新播入场动画。 */
    var generation by mutableIntStateOf(0)
        private set

    fun show(
        state: TeakMorphMenuState,
        items: List<TeakMorphMenuItem>,
        onDismiss: () -> Unit,
        showAnchorCopy: Boolean = true,
        point: Offset? = null,
    ) {
        this.state = state
        this.items = items
        this.onDismiss = onDismiss
        this.showAnchorCopy = showAnchorCopy
        this.point = point
        generation += 1
    }

    /** 只触发收起；内容留到动画结束由宿主清理。 */
    fun hide() {
        state?.close()
    }

    internal fun clear() {
        state = null
        items = emptyList()
        onDismiss = null
        point = null
    }
}

val LocalTeakMorphMenu = staticCompositionLocalOf<TeakMorphMenuController?> { null }

/**
 * Activity 根部只提供控制器；真正的宿主由页面骨架的 menuOverlay 槽渲染，
 * 这样玻璃能采样到同一页面的 Backdrop，又不会被页面自己的录制层包含。
 */
@Composable
fun TeakMorphMenuProvider(content: @Composable () -> Unit) {
    val controller = remember { TeakMorphMenuController() }
    CompositionLocalProvider(LocalTeakMorphMenu provides controller) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}

/** Activity 根部的宿主：先以关闭态组合一帧，再张开，Miuix 才会播入场动画。 */
@Composable
fun TeakMorphMenuOverlay(controller: TeakMorphMenuController) {
    val state = controller.state ?: return
    val items = controller.items
    val dismiss = controller.onDismiss
    val point = controller.point
    val generation = controller.generation

    if (point != null) {
        TeakMorphPointAnchor(state, point)
    }
    LaunchedEffect(generation) {
        // 首帧以未展开状态组合，下一帧再打开，Miuix 的形变才有起点可插值。
        withFrameNanos { }
        state.openWithAnchorCopy(controller.showAnchorCopy)
    }
    LaunchedEffect(state, state.open) {
        if (!state.open && controller.state === state) {
            delay(OverlayReleaseDelayMillis)
            if (controller.state === state && !state.open) controller.clear()
        }
    }
    TeakMorphMenuHost(
        state = state,
        onDismiss = {
            dismiss?.invoke()
            controller.hide()
        },
    ) { close ->
        items.forEach { item ->
            GlassPopupItem(
                text = item.text,
                enabled = item.enabled,
                contentColor = if (item.danger) Color(0xFFE5484D) else Color.Unspecified,
                onClick = {
                    if (!item.enabled) return@GlassPopupItem
                    close { item.onClick() }
                    dismiss?.invoke()
                    controller.hide()
                },
            )
        }
    }
}
