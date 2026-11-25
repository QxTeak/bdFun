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
 * 閿氬畾鍦ㄩ〉闈笂鐨?Miuix 鐜荤拑涓嬫媺鑿滃崟澶栧３銆?
 *
 * 瑙嗛鎿嶄綔鑿滃崟銆佽缃?璇勮鍖洪暱鎸夎彍鍗曞叡鐢ㄨ繖涓€浠?Dialog 绐楀彛銆佸潗鏍囨崲绠椾笌閫€鍦轰繚娲伙紱
 * 鍚勪笟鍔″彧鎻愪緵鍐呭锛岄伩鍏嶆瘡涓皟鐢ㄧ偣鍚勫啓涓€閬?mounted/orientation/origin 閫昏緫銆?
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
    // 鑿滃崟椤硅娲诲埌鏀惰捣鍔ㄧ敾缁撴潫锛歷isible 鍏虫帀鍚庝粛淇濈暀 Dialog锛孧iuix 鎵嶈兘鎾畬寮圭哀銆?
    var mounted by remember { mutableStateOf(false) }
    if (visible) mounted = true
    LaunchedEffect(visible) {
        if (!visible && mounted) {
            delay(480)
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
