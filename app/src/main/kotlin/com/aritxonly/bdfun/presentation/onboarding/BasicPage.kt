package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.onboarding.application.*
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TeakPageScrollState
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsDropdownRow
import com.aritxonly.teak.common.settings.TeakSettingsSwitchRow

@Composable
internal fun BasicPage(settings: OnboardingBasicSettings, actions: OnboardingActions, onBack: () -> Unit, onStart: () -> Unit) {
    val scroll = rememberScrollState()
    val guideState = remember { TeakPageScrollState() }
    LaunchedEffect(scroll.value) { guideState.update(firstVisibleItemIndex = 0, offsetPx = scroll.value.toFloat()) }
    GuidePageFrame(onBack = onBack) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 12.dp)) {
            Spacer(Modifier.height(4.dp))
            TeakLargeTitle(title = "基础设置", pageState = guideState, topSpacer = 0.dp)
            Spacer(Modifier.height(8.dp))
            OnboardingAppearanceCard(settings, actions)
            OnboardingLayoutCard(settings, actions)
            Spacer(Modifier.height(24.dp))
        }
        GuidePrimaryPill(text = "继续", modifier = Modifier.widthIn(max = 336.dp).fillMaxWidth(), onClick = onStart)
        Spacer(Modifier.height(44.dp))
    }
}

/** 界面风格下拉的显示顺序与持久化值映射；HyperOS 4 放最上面。 */
private data class InterfaceStyleOption(val value: Int, val label: String)
private val INTERFACE_STYLE_OPTIONS = listOf(
    InterfaceStyleOption(2, "HyperOS 4"),
    InterfaceStyleOption(0, "哔哩哔哩"),
    InterfaceStyleOption(1, "经典"),
)

@Composable
private fun OnboardingAppearanceCard(settings: OnboardingBasicSettings, actions: OnboardingActions) {
    TeakSettingsCard(title = "外观", modifier = Modifier.fillMaxWidth()) {
        TeakSettingsDropdownRow(
            title = "主题",
            options = listOf("跟随系统", "浅色", "深色"),
            selected = settings.darkMode.coerceIn(0, 2),
            onSelected = { actions.setDarkMode(it) },
        )
        TeakSettingsDropdownRow(
            title = "色彩风格",
            options = listOf("AC 粉", "Hyper 蓝"),
            selected = settings.colorStyle.coerceIn(0, 1),
            onSelected = {
                actions.setColorStyle(it)
            },
        )
        TeakSettingsDropdownRow(
            title = "界面风格",
            // 默认先展示 HyperOS 4；选项顺序与存储值解耦，存储值仍是 0/1/2。
            options = INTERFACE_STYLE_OPTIONS.map { it.label },
            selected = INTERFACE_STYLE_OPTIONS
                .indexOfFirst { it.value == settings.interfaceStyle }
                .coerceAtLeast(0),
            onSelected = { index ->
                INTERFACE_STYLE_OPTIONS.getOrNull(index)?.let { actions.setInterfaceStyle(it.value) }
            },
        )
    }
}

@Composable
private fun OnboardingLayoutCard(settings: OnboardingBasicSettings, actions: OnboardingActions) {
    TeakSettingsCard(title = "布局与效果", modifier = Modifier.fillMaxWidth()) {
        TeakSettingsSwitchRow(
            title = "抬高底栏",
            description = "解决未开启安卓导航条时底栏位置过低",
            checked = settings.bottomBarRaise,
            onCheckedChange = { actions.setBottomBarRaise(it) },
        )
        TeakSettingsSwitchRow(
            title = "回到顶部控件",
            checked = settings.scrollTopEnabled,
            onCheckedChange = { actions.setScrollTopEnabled(it) },
        )
        TeakSettingsSwitchRow(
            title = "高级材质",
            description = "关闭后玻璃控件改为纯色，低端机更流畅",
            checked = settings.blurEnabled,
            onCheckedChange = { actions.setBlurEnabled(it) },
        )
    }
}
