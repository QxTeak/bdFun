package com.aritxonly.bdfun

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.DisposableEffect
import com.aritxonly.bdfun.ui.findHostActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.aritxonly.bdfun.ui.MineTopAppBar
import com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress
import com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial

/** 渐进模糊和按钮都位于采样树外；宽屏顶栏仅保留模糊，设置入口属于不采样的资料栏。 */
@Composable
internal fun BoxScope.MainMineChrome(shell: MainShellState, pageFloat: Float) {
    val widthDp = LocalWindowInfo.current.containerSize.width / LocalDensity.current.density
    if (pageFloat <= 1.01f) return
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findHostActivity()?.window
        val previous = window?.isStatusBarContrastEnforced
        // 主窗口已由 enableEdgeToEdge 铺满；只在我的页去掉系统额外蒙层。
        window?.isStatusBarContrastEnforced = false
        onDispose { if (previous != null) window?.isStatusBarContrastEnforced = previous }
    }
    CompositionLocalProvider(LocalTopBarScrollProgress provides shell.chrome.mine.materialProgress) {
        ProgressiveTopBarMaterial(modifier = Modifier.align(Alignment.TopCenter).graphicsLayer {
            translationX = (2f - pageFloat) * size.width
            alpha = (pageFloat - 1f).coerceIn(0f, 1f)
        }) {
            MineTopAppBar(showSettings = widthDp < 600f) { navigateToSettings(context) }
        }
    }
}
