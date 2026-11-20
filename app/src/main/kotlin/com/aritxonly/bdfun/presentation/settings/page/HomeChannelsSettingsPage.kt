package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.domain.DynamicFeedTab
import com.aritxonly.bdfun.core.domain.DynamicTabPolicy
import com.aritxonly.bdfun.core.domain.HomeChannelPolicy
import com.aritxonly.bdfun.core.model.home.HomeChannel
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsSwitchRow
import com.aritxonly.teak.common.settings.TeakSettingsValueRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页频道设置：显示开关和顺序放在同一张卡片里，顺序紧跟在开关后面。
 * 不单独开一张「频道顺序」卡片，避免同一个列表被拆成两段。
 */
@Composable
internal fun HomeChannelsSettingsSection(settings: SettingsBindings) {
    val order = HomeChannelPolicy.normalizeOrder(settings.homeChannelOrder)
    TeakSettingsCard(title = "首页频道") {
        TeakSettingsValueRow("推荐", "始终开启")
        HomeChannel.entries.drop(1).forEach { channel ->
            TeakSettingsSwitchRow(
                title = channel.title,
                description = if (channel == HomeChannel.Popular) "内容即将上线" else "",
                checked = channel.key in settings.homeChannels,
                onCheckedChange = { enabled ->
                    settings.homeChannels = if (enabled) settings.homeChannels + channel.key
                    else settings.homeChannels - channel.key
                },
            )
        }
        order.forEachIndexed { index, key ->
            val channel = HomeChannel.entries.firstOrNull { it.key == key } ?: return@forEachIndexed
            OrderRow(
                title = channel.title,
                enabled = channel.key in settings.homeChannels,
                canMoveUp = index > 0,
                canMoveDown = index < order.lastIndex,
                onMove = { delta ->
                    settings.homeChannelOrder = movedOrder(order, key, delta)
                },
            )
        }
    }
}

/** 动态页顶部的类型 Tab：开关 + 顺序同一张卡片，「全部」始终开启。 */
@Composable
internal fun DynamicTabsSettingsSection(settings: SettingsBindings) {
    val order = DynamicTabPolicy.normalizeOrder(settings.dynamicFeedTabOrder)
    TeakSettingsCard(title = "动态类型") {
        TeakSettingsValueRow("全部", "始终开启")
        DynamicFeedTab.entries.drop(1).forEach { tab ->
            TeakSettingsSwitchRow(
                title = tab.title,
                checked = tab.key in settings.dynamicFeedTabs,
                onCheckedChange = { enabled ->
                    settings.dynamicFeedTabs = if (enabled) settings.dynamicFeedTabs + tab.key
                    else settings.dynamicFeedTabs - tab.key
                },
            )
        }
        order.forEachIndexed { index, key ->
            val tab = DynamicFeedTab.entries.firstOrNull { it.key == key } ?: return@forEachIndexed
            OrderRow(
                title = tab.title,
                enabled = tab.key in settings.dynamicFeedTabs,
                canMoveUp = index > 0,
                canMoveDown = index < order.lastIndex,
                onMove = { delta -> settings.dynamicFeedTabOrder = movedOrder(order, key, delta) },
            )
        }
    }
}

/** 顺序行：上下箭头只改顺序，是否显示仍由上面的开关决定。 */
@Composable
private fun OrderRow(
    title: String,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = if (enabled) colors.onBackground else colors.onBackgroundVariant,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        OrderArrow(up = true, enabled = enabled && canMoveUp) { onMove(-1) }
        Spacer(Modifier.width(4.dp))
        OrderArrow(up = false, enabled = enabled && canMoveDown) { onMove(1) }
    }
}

@Composable
private fun OrderArrow(up: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val tint = MiuixTheme.colorScheme.onBackgroundVariant
    Box(
        modifier = Modifier
            .size(34.dp)
            .clickable(enabled = enabled, interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = MiuixIcons.ChevronForward,
            contentDescription = if (up) "上移" else "下移",
            tint = if (enabled) tint else tint.copy(alpha = 0.24f),
            modifier = Modifier.size(18.dp).rotate(if (up) -90f else 90f),
        )
    }
}

/** 把 key 在列表里上/下移一位；越界时原样返回。 */
private fun movedOrder(order: List<String>, key: String, delta: Int): List<String> {
    val mutable = order.toMutableList()
    val index = mutable.indexOf(key)
    val target = index + delta
    if (index < 0 || target !in mutable.indices) return order
    mutable.removeAt(index)
    mutable.add(target, key)
    return mutable
}
