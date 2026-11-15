package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.settings.Setting
import com.aritxonly.bdfun.core.model.settings.SettingValue

/** 仅允许已声明的布尔键和整数选项比较；不执行任意表达式。 */
object SettingsVisibility {
    fun valid(condition: String, settings: Map<String, Setting>): Boolean = condition.split("&&").all { term ->
        val parts = term.split('=')
        val setting = settings[parts[0]]
        when (parts.size) {
            1 -> setting?.default is SettingValue.Flag
            2 -> parts[1].toIntOrNull()?.let { value -> setting?.options?.any { it.value == SettingValue.Number(value) } } == true
            else -> false
        }
    }

    fun matches(condition: String, values: Map<String, SettingValue>): Boolean = condition.split("&&").all { term ->
        val parts = term.split('=')
        when (parts.size) {
            1 -> (values[parts[0]] as? SettingValue.Flag)?.value == true
            2 -> parts[1].toIntOrNull()?.let { values[parts[0]] == SettingValue.Number(it) } == true
            else -> false
        }
    }
}
