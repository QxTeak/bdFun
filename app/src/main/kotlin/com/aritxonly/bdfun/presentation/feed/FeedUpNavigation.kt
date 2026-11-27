package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.domain.DynamicFeedTab
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.feature.feed.presentation.FeedState

/**
 * UP 页的卡片要跟着当前分段（全部 / 视频 / 番剧）走。
 *
 * 服务端的分类与 host_mid 互斥（见 `docs/HYPERPILI_DYNAMIC_ALIGNMENT.md`），
 * 所以 UP 页拿到的是该 UP 的完整动态流，分类在这里按附件特征筛：
 * 视频动态带 video / 合集 / 充电标记，番剧动态带 PGC 卡。
 */
internal fun feedItemsForTab(tabKey: String, items: List<FeedItem>): List<FeedItem> = when (tabKey) {
    DynamicFeedTab.Video.key -> items.filter {
        it.video != null || it.isCollection || it.isCharge
    }
    DynamicFeedTab.Pgc.key -> items.filter {
        it.pgcSeasonId > 0L || it.pgcCover.isNotBlank() || it.pgcTitle.isNotBlank()
    }
    else -> items
}

/** 分类筛空了时的提示；「全部」不需要它。 */
internal fun tabEmptyText(tabKey: String): String? = when (tabKey) {
    DynamicFeedTab.Video.key -> "这个 UP 暂时没有视频动态"
    DynamicFeedTab.Pgc.key -> "这个 UP 暂时没有番剧动态"
    else -> null
}

/**
 * 头像行里参与「UP 模式」的顺序：直播 UP、我、关注 UP，和横向列表的排列一致。
 * 「全部动态」不在里面——它是退出键，不参与左右滑动。
 *
 * 注意：UP 页的顶栏虽然不画直播头像，pager 的顺序仍要包含它们，
 * 否则从直播头像点进来时这一页会找不到自己，滑一下就跳到别人。
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

/** 头像在列表里的下标（含「全部动态」），用于把选中项滚进视口。 */
internal fun feedAvatarIndex(
    state: FeedState,
    selfMid: Long?,
    target: Any?,
    includeLive: Boolean = true,
): Int {
    // UP 页的顶栏不排直播头像，下标必须跟着这一页实际的列表算，否则会滚到别的头像上。
    val live = if (includeLive) state.liveUsers.map { it.mid } else emptyList()
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
