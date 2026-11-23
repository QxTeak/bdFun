package com.aritxonly.bdfun.infra.playback

import java.lang.ref.WeakReference

/**
 * 进程内媒体通知仲裁。
 *
 * 视频页存活时，那条 5 键媒体通知由页面会话 [MediaPlaybackSession] 拥有；
 * 后台保活服务 [PlaybackForegroundService] 只是把自己挂在同一个 id 上当前台服务，
 * 不再另发一条 3 键通知——否则离开 App 后通知栏会出现两条控制栏。
 * 页面会话释放（上交播放器后退出视频页）时，通知立刻交回服务。
 *
 * 会话持有 Activity context，所以这里只存弱引用：页面没走正常释放流程时，
 * 也不能让进程级单例把它一直吊住。
 */
internal object PlaybackNotificationArbiter {
    private var pageSessionRef: WeakReference<MediaPlaybackSession>? = null

    /** 当前存活的页面媒体会话；非空时它拥有通知。 */
    val pageSession: MediaPlaybackSession?
        get() = pageSessionRef?.get()

    /** 服务运行期间注册：通知归属发生变化时立刻重新挂前台通知。 */
    @Volatile
    private var onOwnerChanged: (() -> Unit)? = null

    fun attachPageSession(session: MediaPlaybackSession) {
        pageSessionRef = WeakReference(session)
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
     * @return true 表示通知已经归别人（新页面会话或后台服务），调用方不要自行取消那条通知。
     */
    fun releasePageSession(session: MediaPlaybackSession): Boolean {
        val current = pageSession
        // 前后两个视频页交接时，旧会话可能晚一步释放：此时通知已经归新会话，不能跟着取消。
        if (current !== session) return current != null
        pageSessionRef = null
        val listener = onOwnerChanged ?: return false
        listener()
        return true
    }
}
