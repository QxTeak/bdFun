package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakContinuousCornerShape
import com.aritxonly.teak.common.settings.TeakSettingsCard
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 关于页只改变卡片容器的不透明度，文字和控件保持完整对比度。 */
@Composable
internal fun SettingsIndexGroupCard(title: String, aboutProgress: Float?, content: @Composable ColumnScope.() -> Unit) {
    if (aboutProgress == null) {
        TeakSettingsCard(title = title, content = content)
        return
    }
    val colors = MiuixTheme.colorScheme
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    Column(Modifier.padding(top = if (classic) 0.dp else 12.dp)) {
        Text(title, color = colors.onBackgroundVariant, fontSize = 11.5.sp,
            modifier = Modifier.padding(start = if (classic) 12.dp else 14.dp, top = if (classic) 12.dp else 0.dp, bottom = 5.dp))
        Column(
            Modifier.fillMaxWidth()
                .then(
                    if (classic) {
                        Modifier.background(colors.surfaceContainer.copy(alpha = .5f + aboutProgress.coerceIn(0f, 1f) * .5f))
                    } else {
                        Modifier.clip(TeakContinuousCornerShape(22.dp))
                            .background(colors.surfaceContainer.copy(alpha = .5f + aboutProgress.coerceIn(0f, 1f) * .5f))
                    },
                ),
            content = content,
        )
    }
}
