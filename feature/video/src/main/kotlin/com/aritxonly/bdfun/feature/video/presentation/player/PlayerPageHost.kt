package com.aritxonly.bdfun.ui

/** 页面瞬时投影与暂停注册端口；实现继续使用原页面StateFlow与暂停注册表。 */
interface PlayerPageHost {
    val aid: Long
    val cid: Long
    var danmakuProgressMs: Long
    fun requestAutoNext()
    var controlsVisible: Boolean
    var orientationLocked: Boolean
    fun registerPause(owner: Any?, hook: (Boolean) -> Unit)
    fun unregisterPause(hook: (Boolean) -> Unit)
    /** 被后台/其他页面暂停时登记续播动作，回到本页由 Activity 显式调用。 */
    fun setPendingResume(action: (() -> Unit)?)
    fun resumeIfPending()
}

class PlayerDiagnostics(val log: (String, String) -> Unit, val toast: (String) -> Unit)
