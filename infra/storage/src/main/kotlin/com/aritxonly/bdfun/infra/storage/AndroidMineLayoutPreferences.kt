package com.aritxonly.bdfun.infra.storage

import android.content.Context
import com.aritxonly.bdfun.core.domain.MineLayoutPreferences
import com.aritxonly.bdfun.core.model.settings.MineLayout

class AndroidMineLayoutPreferences(context: Context) : MineLayoutPreferences {
    private val prefs = context.applicationContext.getSharedPreferences("mine_layout", Context.MODE_PRIVATE)
    override fun read() = MineLayout(
        prefs.getBoolean("history", MineLayout().history),
        prefs.getBoolean("favorites", MineLayout().favorites),
        prefs.getBoolean("watch_later", MineLayout().watchLater),
    )
    override fun write(layout: MineLayout) {
        prefs.edit().putBoolean("history", layout.history).putBoolean("favorites", layout.favorites)
            .putBoolean("watch_later", layout.watchLater).apply()
    }
}
