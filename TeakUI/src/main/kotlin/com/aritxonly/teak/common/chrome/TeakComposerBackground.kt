package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritxonly.teak.common.theme.TeakDesign
import com.aritxonly.teak.common.theme.TeakUiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 两种输入栏共用背景；保持背景与内容为兄弟节点，不接管 IME 或内容布局。 */
@Composable
fun BoxScope.TeakComposerBackground() {
    when (TeakUiStyle.design) {
        TeakDesign.Classic -> {
            Box(Modifier.matchParentSize().teakClassicBarGlass())
            Box(Modifier.matchParentSize().background(
                MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f),
            ))
        }
        TeakDesign.Modern -> Unit
    }
}
