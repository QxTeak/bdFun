package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.application.RecommendationOptimizer
import com.aritxonly.teak.common.settings.*

@Composable
internal fun SettingsRecommendationRow(entry: com.aritxonly.bdfun.core.model.settings.Setting, optimizer: RecommendationOptimizer, notify: (String) -> Unit) {
    val mode by optimizer.state.collectAsState()
    when (entry.prefName) {
        "recommendation_toggle" -> TeakSettingsSwitchRow(
            title = entry.title,
            description = entry.subtitle.orEmpty(),
            checked = mode.enabled,
            onCheckedChange = { if (mode.ready) optimizer.setEnabled(it) else notify("正在读取推荐设置") },
        )
        "recommendation_clear" -> TeakSettingsActionRow(title = entry.title, trailing = "仅当前账号") {
            if (mode.ready) {
                optimizer.clear()
                notify("已清除本机推荐学习记录")
            } else notify("正在读取推荐设置")
        }
        "recommendation_error" -> mode.error?.let { TeakSettingsValueRow(title = entry.title, value = it) }
    }
}
