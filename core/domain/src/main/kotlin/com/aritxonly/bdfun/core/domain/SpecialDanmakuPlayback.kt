package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.DanmakuItem

/** 单播放器序列读取，仓储负责保留覆盖当前位置的长弹幕和短期预读。 */
fun interface SpecialDanmakuPlayback {
    suspend fun readWindow(positionMs: Long): List<DanmakuItem>
}
