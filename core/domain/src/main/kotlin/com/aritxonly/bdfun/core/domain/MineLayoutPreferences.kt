package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.settings.MineLayout

interface MineLayoutPreferences {
    fun read(): MineLayout
    fun write(layout: MineLayout)
}
