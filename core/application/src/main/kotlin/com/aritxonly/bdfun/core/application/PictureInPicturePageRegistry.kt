package com.aritxonly.bdfun.core.application

import java.util.concurrent.CopyOnWriteArrayList

/**
 * 记录当前处于画中画的视频页。
 * 画中画页面在系统的独立任务里继续存活；用户回主界面再开新视频时，旧的小窗必须关闭，
 * 否则两个视频页同时存在（且旧页面关闭时还会把播放器交给唱片，出现两路声音）。
 * 只持有页面登记的关闭回调，不持有 Android 类型。
 */
class PictureInPicturePageRegistry {
    private data class Entry(val owner: Any, val close: () -> Unit)
    private val entries = CopyOnWriteArrayList<Entry>()

    fun enter(owner: Any, close: () -> Unit) {
        entries.removeAll { it.owner === owner }
        entries.add(Entry(owner, close))
    }

    fun exit(owner: Any) { entries.removeAll { it.owner === owner } }

    /** 新视频页打开时调用：关闭其它页面的画中画，自身（例如从小窗展开）不受影响。 */
    fun closeOthers(owner: Any) {
        val others = entries.filter { it.owner !== owner }
        entries.removeAll(others.toSet())
        others.forEach { runCatching { it.close() } }
    }
}
