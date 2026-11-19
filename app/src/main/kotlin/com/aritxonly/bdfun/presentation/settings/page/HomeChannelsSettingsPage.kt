package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
 * 首页频道设置：不再单独开页，直接作为「首页、动态与搜索」分类里的一小段，
 * 上面是显示开关，下面是同一份列表的顺序调整。
 */
@Composable
internal fun HomeChannelsSettingsSection(settings: SettingsBindings) {
    val order = HomeChannelPolicy.normalizeOrder(settings.homeChannelOrder)
    Column(Modifier.fillMaxWidth()) {
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
        }
        TeakSettingsCard(title = "频道顺序") {
            order.forEachIndexed { index, key ->
                val channel = HomeChannel.entries.firstOrNull { it.key == key } ?: return@forEachIndexed
                val enabled = channel.key in settings.homeChannels
                HomeChannelOrderRow(
                    title = channel.title,
                    enabled = enabled,
                    canMoveUp = index > 0,
                    canMoveDown = index < order.lastIndex,
                    onMoveUp = { moveChannel(settings, order, key, -1) },
                    onMoveDown = { moveChannel(settings, order, key, 1) },
                )
            }
        }
    }
}

/** 上下箭头只改顺序，是否显示仍由上面的开关决定。 */
@Composable
private fun HomeChannelOrderRow(
    title: String,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
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
        OrderArrow(up = true, enabled = enabled && canMoveUp, onClick = onMoveUp)
        Spacer(Modifier.width(4.dp))
        OrderArrow(up = false, enabled = enabled && canMoveDown, onClick = onMoveDown)
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

private fun moveChannel(settings: SettingsBindings, order: List<String>, key: String, delta: Int) {
    val mutable = order.toMutableList()
    val index = mutable.indexOf(key)
    val target = index + delta
    if (index < 0 || target !in mutable.indices) return
    mutable.removeAt(index)
    mutable.add(target, key)
    settings.homeChannelOrder = HomeChannelPolicy.normalizeOrder(mutable)
}
