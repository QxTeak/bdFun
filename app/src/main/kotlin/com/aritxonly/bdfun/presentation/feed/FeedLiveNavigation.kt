package com.aritxonly.bdfun.ui


internal fun openFeedLive(context: android.content.Context, roomId: Long) {
    if (roomId <= 0) return
    runCatching {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://live.bilibili.com/$roomId")))
    }.onFailure { context.showToast("无法打开直播间") }
}
