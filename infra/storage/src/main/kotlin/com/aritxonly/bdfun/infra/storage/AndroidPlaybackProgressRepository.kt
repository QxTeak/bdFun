package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.domain.resumePlaybackProgress
import com.aritxonly.bdfun.core.domain.playbackProgressEvictions
import com.aritxonly.bdfun.core.domain.playbackProgressDecision
import android.content.Context
import android.content.SharedPreferences
import com.aritxonly.bdfun.core.domain.*

/** 原文件名、键名和 position,duration,timestamp 字符串不变，可直接读取旧安装数据。 */
class AndroidPlaybackProgressRepository(context: Context?, private val clock: () -> Long = System::currentTimeMillis) : PlaybackProgressRepository {
    private val prefs = context?.applicationContext?.getSharedPreferences("playback_progress", Context.MODE_PRIVATE)
    @Volatile private var writesSincePrune = 0
    @Volatile private var pruned = false
    // 清理旧进度放到第一次写入时做：构造期在 App 启动主线程上，不能在这里读整份 XML。
    private fun pruneOnce(store: SharedPreferences) {
        if (pruned) return
        pruned = true
        prune(store)
    }
    override fun resumeFor(bvid: String): Long {
        val store = prefs ?: return 0L
        if (bvid.isBlank()) return 0L
        return resumePlaybackProgress(store.getString("p_$bvid", null))
    }
    override fun save(bvid: String, positionMs: Long, durationMs: Long) {
        val store = prefs ?: return
        if (bvid.isBlank()) return
        pruneOnce(store)
        when (playbackProgressDecision(positionMs, durationMs)) {
            ProgressSaveDecision.Skip -> return
            ProgressSaveDecision.Clear -> { clear(bvid); return }
            ProgressSaveDecision.Write -> store.edit().putString("p_$bvid", "$positionMs,$durationMs,${clock()}").apply()
        }
        writesSincePrune++
        if (writesSincePrune >= 100) { writesSincePrune = 0; prune(store) }
    }
    override fun clear(bvid: String) {
        if (bvid.isNotBlank()) prefs?.edit()?.remove("p_$bvid")?.apply()
    }
    private fun prune(store: SharedPreferences) {
        val entries = store.all.filterKeys { it.startsWith("p_") }.mapValues { it.value as? String }
        val keys = playbackProgressEvictions(entries)
        if (keys.isEmpty()) return
        val editor = store.edit()
        keys.forEach { editor.remove(it) }
        editor.apply()
    }
}
