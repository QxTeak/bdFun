package com.aritxonly.bdfun.infra.playback

/**
 * 进程内媒体通知仲裁。
 *
 * 视频页存活时，那条 5 键媒体通知由页面会话 [MediaPlaybackSession] 拥有；
 * 后台保活服务 [PlaybackForegroundService] 只是把自己挂在同一个 id 上当前台服务，
 * 不再另发一条 3 键通知——否则离开 App 后通知栏会出现两条控制栏。
 * 页面会话释放（上交播放器后退出视频页）时，通知立刻交回服务。
 */
internal object PlaybackNotificationArbiter {
    /** 当前存活的页面媒体会话；非空时它拥有通知。 */
    @Volatile
    var pageSession: MediaPlaybackSession? = null
        private set

    /** 服务运行期间注册：通知归属发生变化时立刻重新挂前台通知。 */
    @Volatile
    private var onOwnerChanged: (() -> Unit)? = null

    fun attachPageSession(session: MediaPlaybackSession) {
        pageSession = session
        onOwnerChanged?.invoke()
    }

    fun registerOwnerChangedListener(action: () -> Unit) {
        onOwnerChanged = action
    }

    fun unregisterOwnerChangedListener() {
        onOwnerChanged = null
    }

    /**
     * 页面会话释放。
     * @return true 表示已有服务接管通知，调用方不要再自行取消那条通知。
     */
    fun releasePageSession(session: MediaPlaybackSession): Boolean {
        if (pageSession !== session) return false
        pageSession = null
        val changed = onOwnerChanged ?: return false
        changed()
        return true
    }
}
