package com.aritxonly.bdfun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 跨 Activity 的「回到首页」请求。
 *
 * 视频页是独立 Activity，收起态的回首页键需要把主界面拉回推荐页；
 * 但 Compose 的 tab 状态只存在于主界面自己的组合里，所以用进程级请求
 * 让主壳在被唤起后读取一次。请求只在真正被消费时清除，主界面重建也不会丢。
 */
internal class MainTabRequestStateHolder {
    var pendingHome by mutableStateOf(false)
        private set

    fun requestHome() {
        pendingHome = true
    }

    fun consumeHome() {
        pendingHome = false
    }
}
