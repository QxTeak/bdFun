package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.LocalProgressiveTopBarMaterialHosted
import com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.teakContentControlShadow
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 共用一个胶囊，输入和开关保持独立点击区域，不互相触发。 */
@Composable
internal fun VideoDanmakuControl(
    compact: Boolean, enabled: Boolean, onOpenInput: () -> Unit, onToggle: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val material = LocalTeakMaterialSpec.current
    val progress = if (LocalProgressiveTopBarMaterialHosted.current) LocalTopBarButtonMaterialProgress.current else 1f
    TeakGlassSurface(
        modifier = Modifier.teakContentControlShadow(Capsule(), progress).height(40.dp),
        shape = Capsule(),
        recipe = TeakGlassRecipes.floatingNavigation(material.fineTuning),
        materialAlpha = progress,
        // 与 TeakPagerTabs / segment 共用同一平面底色和材质过渡，避免未显现时透明。
        flatBackgroundAlpha = com.aritxonly.teak.common.material.TeakGlassDefaults.flatBackgroundAlpha(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.fillMaxHeight()
                    .clickable(role = Role.Button, onClick = onOpenInput)
                    .padding(start = if (compact) 12.dp else 14.dp, end = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("点我发弹幕", color = colors.onBackground.copy(alpha = 0.88f),
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            DanmakuInlineToggle(enabled, onToggle)
        }
    }
}

@Composable
private fun DanmakuInlineToggle(enabled: Boolean, onToggle: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier.width(40.dp).fillMaxHeight()
            .semantics {
                contentDescription = "弹幕开关"
                stateDescription = if (enabled) "已开启" else "已关闭"
            }
            .toggleable(value = enabled, role = Role.Switch, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(32.dp).background(
            if (enabled) colors.primary.copy(alpha = 0.92f) else colors.onBackground.copy(alpha = 0.075f),
            Capsule(),
        ), contentAlignment = Alignment.Center) {
            Text("弹", color = if (enabled) colors.onPrimary else colors.onBackground,
                fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
