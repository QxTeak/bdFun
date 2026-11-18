package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.DanmakuCommand
import com.aritxonly.bdfun.core.model.video.DanmakuCommandKind

data class DanmakuCommandSelection(val aid: Long, val cid: Long, val command: DanmakuCommand, val value: Int) {
    val valid: Boolean get() = aid > 0 && cid > 0 && command.interactionId > 0 &&
        command.options.any { it.value == value } && when (command.kind) {
            DanmakuCommandKind.Vote -> value > 0
            DanmakuCommandKind.Grade -> value in 2..10 && value % 2 == 0
            else -> false
        }
}

fun activeDanmakuCommand(items: List<DanmakuCommand>, dismissed: Set<Long>, positionMs: Long): DanmakuCommand? =
    items.firstOrNull { it.id !in dismissed && positionMs >= it.startMs && positionMs - it.startMs < it.durationMs }
