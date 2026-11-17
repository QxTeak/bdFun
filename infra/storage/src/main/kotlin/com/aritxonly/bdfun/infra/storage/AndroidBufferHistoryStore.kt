package com.aritxonly.bdfun.infra.storage

import android.content.Context
import com.aritxonly.bdfun.core.domain.buffering.BufferHistoryStore
import com.aritxonly.bdfun.core.domain.buffering.WatchHistory

/** 版本化且按账号隔离；游客仅进程内学习，写入只发生在会话结束。 */
class AndroidBufferHistoryStore(context: Context) : BufferHistoryStore {
    private val prefs = context.applicationContext.getSharedPreferences("buffer_history_v1", Context.MODE_PRIVATE)
    private var guest = WatchHistory()

    @Synchronized override fun read(account: String): WatchHistory {
        if (account.isBlank()) return guest
        return BufferHistoryCodec.decode(prefs.getString(account, null))
    }

    @Synchronized override fun observe(account: String, watchedMs: Long, abandoned: Boolean) {
        val updated = read(account).observe(watchedMs, abandoned)
        if (account.isBlank()) guest = updated else {
            prefs.edit().putString(account,
                BufferHistoryCodec.encode(updated)).apply()
        }
    }
}
