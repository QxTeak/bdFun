package com.aritxonly.teak.common.modal

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
import androidx.compose.ui.geometry.Offset
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
import com.aritxonly.teak.common.theme.TeakUiStyle
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassPopupSizing

/**
 * 锚定在页面上的 Miuix 玻璃下拉菜单外壳。
 *
 * 视频操作菜单、设置/评论区长按菜单共用这一份 Dialog 窗口、坐标换算与退场保活；
 * 各业务只提供内容，避免每个调用点各写一遍 mounted/orientation/origin 逻辑。
 */
@Composable
fun TeakAnchoredGlassMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    anchorInWindow: Rect,
    modifier: Modifier = Modifier,
    minWidth: Dp = 168.dp,
    maxWidth: Dp = 300.dp,
    maxHeight: Dp = 420.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 菜单项要活到收起动画结束：visible 关掉后仍保留 Dialog，Miuix 才能播完弹簧。
    var mounted by remember { mutableStateOf(false) }
    if (visible) mounted = true
    LaunchedEffect(visible) {
        if (!visible && mounted) {
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
                var origin by remember { mutableStateOf(Offset.Zero) }
                androidx.compose.runtime.SideEffect {
                    val location = IntArray(2).also { view.getLocationOnScreen(it) }
                    origin = Offset(location[0].toFloat(), location[1].toFloat())
                }
                Box(
                    modifier = Modifier.fillMaxSize().onGloballyPositioned {
                        if (origin == Offset.Zero) origin = it.positionInWindow()
                    },
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
                        backdrop = if (TeakUiStyle.isClassic) null else backdrop,
                        modifier = modifier,
                        sizing = GlassPopupSizing(minWidth = minWidth, maxWidth = maxWidth, maxHeight = maxHeight),
                        content = content,
                    )
                }
            }
        }
    }
}
