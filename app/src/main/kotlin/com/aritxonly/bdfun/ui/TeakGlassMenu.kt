package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.settings.hostActivity
import com.aritxonly.teak.common.settings.matchSystemBars
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassPopupSizing

/**
 * 设置页二级菜单同款的 Miuix 玻璃下拉菜单，可锚定到任意按钮 / 评论位置。
 *
 * 独立 Dialog 窗口：采样页面 backdrop 时不会和页面自身形成递归；
 * 位置用窗口坐标传入，内部自动换算到 Dialog 的坐标系。
 */
@Composable
fun TeakGlassMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    anchorInWindow: Rect,
    modifier: Modifier = Modifier,
    minWidth: Dp = 180.dp,
    maxWidth: Dp = 300.dp,
    maxHeight: Dp = 460.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    var mounted by remember { mutableStateOf(false) }
    if (visible) mounted = true
    LaunchedEffect(visible) {
        if (!visible && mounted) {
            // 等 Miuix 的退场动画跑完再销毁 Dialog。
            delay(320)
            if (!visible) mounted = false
        }
    }
    if (!mounted) return

    val backdrop = LocalTeakBackdrop.current
    val orientation = LocalConfiguration.current.orientation
    key(orientation) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false,
            ),
        ) {
            val view = LocalView.current
            val hostActivity = LocalContext.current.hostActivity()
            val dialogWindow = (view.parent as? DialogWindowProvider)?.window
            androidx.compose.runtime.SideEffect {
                dialogWindow?.setDimAmount(0f)
                dialogWindow?.setBackgroundDrawable(
                    android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
                )
                matchSystemBars(dialogWindow, hostActivity?.window)
            }
            Box(modifier = Modifier.fillMaxSize()) {
                var origin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { origin = it.positionInWindow() },
                ) {
                    val anchor = Rect(
                        left = anchorInWindow.left - origin.x,
                        top = anchorInWindow.top - origin.y,
                        right = anchorInWindow.right - origin.x,
                        bottom = anchorInWindow.bottom - origin.y,
                    )
                    GlassDropdownPopup(
                        show = visible,
                        onDismissRequest = onDismiss,
                        anchorBounds = anchor,
                        backdrop = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) null else backdrop,
                        modifier = modifier,
                        sizing = GlassPopupSizing(
                            minWidth = minWidth,
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                        ),
                        content = content,
                    )
                }
            }
        }
    }
}
