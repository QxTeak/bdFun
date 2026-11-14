package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.settings.SettingValue
import com.aritxonly.bdfun.feature.settings.application.IndexedSettingsStateHolder
import com.aritxonly.bdfun.feature.settings.application.SettingsSplitPolicy
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsSplitLayout(session: SettingsBrowserSession, values: Map<String, SettingValue>,
    runtime: SettingsPageRuntime, controller: IndexedSettingsStateHolder) {
    TeakBareTheme(runtime.settings) { _, _ ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val total = maxWidth.value
            val fraction = session.navigation.splitFraction
            val left = SettingsSplitPolicy.leftWidth(total, fraction)
            val density = LocalDensity.current
            val drag: (Float) -> Unit = { delta ->
                session.browser.resize(total, delta)
            }
            Row(Modifier.fillMaxSize()) {
                SettingsBrowserPane(session, values, runtime, controller, SettingsPane.Categories,
                    Modifier.width(left.dp).fillMaxHeight().clipToBounds())
                SettingsSplitDivider(left / (total - SettingsSplitPolicy.DividerDp).coerceAtLeast(1f), drag, { session.browser.split(SettingsSplitPolicy.DefaultFraction) }, Modifier
                    .width(SettingsSplitPolicy.DividerDp.dp).fillMaxHeight()
                    .pointerInput(total, density.density) {
                        detectDragGestures { change, amount -> change.consume(); drag(amount.x / density.density) }
                    })
                SettingsBrowserPane(session, values, runtime, controller, SettingsPane.Details,
                    Modifier.weight(1f).fillMaxHeight().clipToBounds())
            }
        }
    }
}

@Composable
private fun SettingsSplitDivider(fraction: Float, drag: (Float) -> Unit, reset: () -> Unit, modifier: Modifier) {
    // 分隔线有独立热区和键盘/无障碍操作，不夺取左右栏的纵向滚动。
    Box(modifier.semantics {
        contentDescription = "调整设置分栏宽度"
        stateDescription = "左栏${(fraction * 100).toInt()}%"
        customActions = listOf(
            CustomAccessibilityAction("增大左栏") { drag(32f); true },
            CustomAccessibilityAction("缩小左栏") { drag(-32f); true },
            CustomAccessibilityAction("恢复3比7") { reset(); true },
        )
    }.onKeyEvent {
        if (it.type != KeyEventType.KeyDown) false else when (it.key) {
            Key.DirectionLeft -> { drag(-32f); true }
            Key.DirectionRight -> { drag(32f); true }
            Key.MoveHome -> { reset(); true }
            else -> false
        }
    }.focusable(), contentAlignment = Alignment.Center) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(MiuixTheme.colorScheme.outline.copy(alpha = .2f)))
        Box(Modifier.width(4.dp).height(36.dp).background(MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = .4f)))
    }
}
