package com.aritxonly.bdfun.core.application

data class MediaQueueEntry(val mediaId: String, val title: String, val subtitle: String = "")
interface SystemMediaControls {
    fun toggle()
    fun seekBy(deltaMs: Long)
    fun previous()
    fun next()
}

/** 前台系统会话端口；队列与状态更新不依赖 Android 媒体实现。 */
interface ForegroundMediaSession : SystemMediaControls {
    fun updateQueue(entries: List<MediaQueueEntry>, currentIndex: Int)
    fun onPlayerState(playing: Boolean, buffering: Boolean, positionMs: Long, durationMs: Long, speed: Float)
    fun release()
}

/** 系统广播仅定位当前会话；旧会话释放不能注销新会话。 */
class MediaControlsRegistry {
    @Volatile var current: SystemMediaControls? = null
        private set
    fun attach(controls: SystemMediaControls) { current = controls }
    fun detach(controls: SystemMediaControls) { if (current === controls) current = null }
}

interface BackgroundPlaybackPort {
    val playbackSnapshot: PlaybackSnapshot
    val title: String
    val author: String
    val cover: String
    val playing: Boolean
    val positionMs: Long
    val durationMs: Long
    fun next()
    fun previous()
    fun togglePlayPause()
    fun exit()
}
