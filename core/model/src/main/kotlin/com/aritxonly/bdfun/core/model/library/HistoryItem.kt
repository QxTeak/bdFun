package com.aritxonly.bdfun.core.model.library

data class HistoryItem(
    val bvid: String,
    val title: String,
    val cover: String,
    val author: String,
    val viewedAt: Long,
    val progress: Int,
    val duration: Int,
    /** 播放量 / 弹幕数（接口里的 stat 字段，历史与稍后再看都有） */
    val play: Int = 0,
    val danmaku: Int = 0,
    /** 稿件 aid：批量删除历史 / 稍后再看时要用；历史游标接口放在 history.oid 里。 */
    val aid: Long = 0,
    val cid: Long = 0,
)
