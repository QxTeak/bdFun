package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.domain.buffering.WatchHistory

internal object BufferHistoryCodec {
    fun decode(raw: String?): WatchHistory {
        val tokens = raw?.split(',') ?: return WatchHistory()
        if (tokens.size != 13) return WatchHistory()
        val values = tokens.map { it.toDoubleOrNull() ?: return WatchHistory() }
        if (values.any { !it.isFinite() || it < 0 }) return WatchHistory()
        return WatchHistory(values.take(6), values.subList(6, 12), values[12])
    }

    fun encode(history: WatchHistory): String =
        (history.exposureSeconds + history.exits + history.samples).joinToString(",")
}
