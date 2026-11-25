package com.aritxonly.teak.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.glass.GlassPopupItem

/** 一条玻璃形变菜单项（与具体业务无关）。 */
data class TeakMorphMenuItem(
    val text: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * 全 Activity 共用的形变菜单控制器：任意页面的省略号只需要自己的
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

    fun show(state: TeakMorphMenuState, items: List<TeakMorphMenuItem>, onDismiss: () -> Unit) {
        this.state = state
        this.items = items
        this.onDismiss = onDismiss
    }

    fun hide() {
        state?.close()
        state = null
        items = emptyList()
        onDismiss = null
    }
}

val LocalTeakMorphMenu = staticCompositionLocalOf<TeakMorphMenuController?> { null }

/** Activity 根部的宿主；收起动画期间保留最后一份内容。 */
@Composable
fun TeakMorphMenuOverlay(controller: TeakMorphMenuController) {
    val state = controller.state
    val items = controller.items
    val dismiss = controller.onDismiss
    if (state == null) return
    TeakMorphMenuHost(
        state = state,
        onDismiss = { dismiss?.invoke(); controller.hide() },
    ) { close ->
        items.forEach { item ->
            GlassPopupItem(
                text = item.text,
                enabled = item.enabled,
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
