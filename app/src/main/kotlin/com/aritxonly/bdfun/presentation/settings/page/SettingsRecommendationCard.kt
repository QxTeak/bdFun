package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.application.RecommendationOptimizer
import com.aritxonly.teak.common.settings.*

@Composable
internal fun SettingsRecommendationCard(optimizer: RecommendationOptimizer, notify: (String) -> Unit) {
    val mode by optimizer.state.collectAsState()
    TeakSettingsCard(title = "首页推荐") {
        TeakSettingsSwitchRow(
            title = "推荐算法优化",
            description = "根据本机观看反馈微调推荐顺序，减少重复。关闭后下次刷新恢复原推荐方式。",
            checked = mode.enabled,
            onCheckedChange = { if (mode.ready) optimizer.setEnabled(it) else notify("正在读取推荐设置") },
        )
        TeakSettingsActionRow(title = "清除推荐学习记录", trailing = "仅当前账号") {
            if (mode.ready) {
                optimizer.clear()
                notify("已清除本机推荐学习记录")
            } else notify("正在读取推荐设置")
        }
        mode.error?.let { TeakSettingsValueRow(title = "推荐记录", value = it) }
    }
}
