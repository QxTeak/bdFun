package com.aritxonly.bdfun.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.teakClassicBarGlass
import com.aritxonly.teak.common.chrome.teakProgressiveBarBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun androidx.compose.foundation.layout.BoxScope.HomeTopBarBackground(classic: Boolean) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                if (classic) {
                    // 经典顶栏常驻，整条走 Miuix 玻璃模糊；不再画分割线。
                    Modifier.teakClassicBarGlass()
                } else {
                    Modifier.teakProgressiveBarBlur()
                },
            ),
    )
    if (classic) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f)),
        )
    }
}
