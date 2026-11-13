package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.domain.MineLayoutPreferences
import com.aritxonly.bdfun.core.model.settings.MineLayout
import org.junit.Assert.*
import org.junit.Test

class MineLayoutStateHolderTest {
    private class Store : MineLayoutPreferences {
        var saved = MineLayout()
        override fun read() = saved
        override fun write(layout: MineLayout) { saved = layout }
    }
    @Test fun independentChoicesSurviveControllerRecreation() {
        val store = Store()
        val holder = MineLayoutStateHolder(store)
        assertEquals(MineLayout(false, false, false), holder.state.value)
        holder.setHistory(true); holder.setFavorites(true); holder.setWatchLater(true)
        holder.setFavorites(false)
        assertEquals(MineLayout(true, false, true), MineLayoutStateHolder(store).state.value)
        assertEquals(store.saved, holder.state.value)
    }
}
