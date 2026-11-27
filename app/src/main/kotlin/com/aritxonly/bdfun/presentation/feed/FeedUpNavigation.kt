package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.feature.feed.presentation.FeedState

/**
 * 头像行里参与「UP 模式」的顺序：直播 UP、我、关注 UP，和横向列表的排列一致。
 * 「全部动态」不在里面——它是退出键，不参与左右滑动。
 */
internal fun feedUpMids(state: FeedState, selfMid: Long?): List<Long> {
    val live = state.liveUsers.map { it.mid }
    val me = selfMid?.takeIf { it !in live }
    val authors = state.authors.map { it.mid }.filterNot { it in live }
    return (live + listOfNotNull(me) + authors).distinct()
}

/** 退出 UP 模式：回到这一页自己的分类（全部/视频/番剧），而不是一律切成「全部」。 */
internal suspend fun FeedState.exitUpFocus(tabKey: String, loggedIn: Boolean) {
    if (filterMid == null) return
    switchTo(tabKey, loggedIn)
}

/** 头像在列表里的下标（含「全部动态」），用于把选中项滚进视口。 */internal fun feedAvatarIndex(state: FeedState, selfMid: Long?, target: Any?): Int {
    val live = state.liveUsers.map { it.mid }
    val me = selfMid?.takeIf { it !in live }
    val authors = state.authors.map { it.mid }.filterNot { it in live }
    if (target == FeedAllAvatarKey) return live.size
    val mid = target as? Long ?: return -1
    if (mid in live) return live.indexOf(mid)
    if (mid == me) return live.size + 1
    val offset = live.size + 1 + if (me != null) 1 else 0
    val index = authors.indexOf(mid)
    return if (index >= 0) offset + index else -1
}
