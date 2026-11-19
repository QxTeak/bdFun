package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class SettingsValuesStateHolder {
    private val mutable = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutable.asStateFlow()

    fun <T> cell(key: String, initial: T): SettingsCell<T> {
        publish(key, initial)
        return SettingsCell(initial) { publish(key, it) }
    }

    fun invalidate() { mutable.update { it.copy(revision = it.revision + 1) } }

    private fun <T> publish(key: String, value: T) {
        val boxed = when (value) {
            is Boolean -> SettingValue.Flag(value)
            is Int -> SettingValue.Number(value)
            is Float -> SettingValue.Decimal(value)
            is String -> SettingValue.Text(value)
            is Set<*> -> SettingValue.TextSet(value.filterIsInstance<String>().toSet())
            // 有序列表用逗号串投影，保留先后顺序（Set 投影会丢顺序）。
            is List<*> -> SettingValue.Text(value.filterIsInstance<String>().joinToString(","))
            else -> error("不支持的设置类型：$key")
        }
        mutable.update { it.copy(values = it.values + (key to boxed)) }
    }
}

internal class SettingsCell<T>(initial: T, private val changed: (T) -> Unit) {
    @Volatile var value: T = initial
        set(value) { field = value; changed(value) }
}

/** 同旧 Handler：最后一次浮点修改后 250ms 合并落盘。关闭作用域前显式 flush。 */
internal class FloatSettingsWriter(private val prefs: SettingsPreferences, private val scope: CoroutineScope) {
    private val pending = LinkedHashMap<String, Float>()
    private var job: Job? = null
    fun put(key: String, value: Float) {
        pending[key] = value
        job?.cancel()
        job = scope.launch { delay(250L); flush() }
    }
    fun flush() {
        job?.cancel()
        job = null
        if (pending.isEmpty()) return
        val editor = prefs.edit()
        pending.forEach { (key, value) -> editor.putFloat(key, value) }
        pending.clear()
        editor.apply()
    }
}

internal abstract class SettingsGroupStateHolder(
    protected val prefs: SettingsPreferences,
    protected val values: SettingsValuesStateHolder,
    protected val floats: FloatSettingsWriter,
    protected val codec: SearchHistoryCodec,
) {
    protected fun <T> stateCell(key: String, initial: T) = values.cell(key, initial)
    protected fun persistFloatEventually(key: String, value: Float) = floats.put(key, value)
}
