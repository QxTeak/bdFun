package com.aritxonly.bdfun.ui


/**
 * 动态页里的直播入口（头像卡 / 侧栏）统一进站内直播间。
 * 以前这里直接 ACTION_VIEW 扔给浏览器，点一下就跳出 App，和直播页的处理不一致。
 */
internal fun openFeedLive(context: android.content.Context, roomId: Long) {
    if (roomId <= 0) return
    com.aritxonly.bdfun.navigateToLiveRoom(context, roomId)
}
