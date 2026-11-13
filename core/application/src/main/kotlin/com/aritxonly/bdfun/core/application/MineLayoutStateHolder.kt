package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.domain.MineLayoutPreferences
import com.aritxonly.bdfun.core.model.settings.MineLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MineLayoutStateHolder(private val preferences: MineLayoutPreferences) {
    private val mutable = MutableStateFlow(preferences.read())
    val state = mutable.asStateFlow()
    fun setHistory(enabled: Boolean) = save(mutable.value.copy(history = enabled))
    fun setFavorites(enabled: Boolean) = save(mutable.value.copy(favorites = enabled))
    fun setWatchLater(enabled: Boolean) = save(mutable.value.copy(watchLater = enabled))
    private fun save(layout: MineLayout) {
        preferences.write(layout)
        mutable.value = layout
    }
}
