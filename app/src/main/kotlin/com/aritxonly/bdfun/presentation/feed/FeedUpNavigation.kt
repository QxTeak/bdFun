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

/** 左右滑动切换 UP：越过两端就停在原地，不绕回「全部动态」。 */
internal suspend fun FeedState.stepUpFocus(delta: Int, selfMid: Long?) {
    val current = filterMid ?: return
    val order = feedUpMids(this, selfMid)
    val next = order.getOrNull(order.indexOf(current) + delta) ?: return
    if (next != current) filterTo(next)
}

/** 退出 UP 模式：回到这一页自己的分类（全部/视频/番剧），而不是一律切成「全部」。 */
internal suspend fun FeedState.exitUpFocus(tabKey: String, loggedIn: Boolean) {
    if (filterMid == null) return
    switchTo(tabKey, loggedIn)
}

/** 头像在列表里的下标（含「全部动态」），用于把选中项滚进视口。 */
internal fun feedAvatarIndex(state: FeedState, selfMid: Long?, target: Any?): Int {
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

/**
 * UP 切换的方向：按两人在头像行里的先后决定。
 *
 * +1 表示新内容从右侧滑入（目标在右边），-1 从左侧滑入；进入/退出「全部动态」
 * 或顺序拿不到时返回 0，只淡入不位移——那是换模式，不是换 UP。
 */
internal fun feedUpSwitchDirection(previous: Long?, current: Long?, order: List<Long>): Int {
    if (previous == null || current == null || previous == current) return 0
    val from = order.indexOf(previous)
    val to = order.indexOf(current)
    if (from < 0 || to < 0) return 0
    return (to - from).coerceIn(-1, 1)
}
