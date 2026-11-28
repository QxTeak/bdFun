package com.aritxonly.bdfun.core.application

import kotlinx.coroutines.flow.StateFlow

enum class PlaybackPhase { Idle, Buffering, Ready, Ended }
data class PlaybackFailure(val code: String, val message: String?)
enum class PlaybackVideoOutputMode { Standard, Hdr, DolbyVision }

data class PlaybackVideoTarget(
    val qualityId: Int,
    val mode: PlaybackVideoOutputMode,
    val upgradeInBackground: Boolean = false,
)

data class PlaybackSnapshot(
    val playing: Boolean = false, val playWhenReady: Boolean = false,
    val phase: PlaybackPhase = PlaybackPhase.Idle, val ready: Boolean = false, val ended: Boolean = false,
    val positionMs: Long = 0, val bufferedMs: Long = 0, val durationMs: Long = 0,
    val speed: Float = 1f, val error: PlaybackFailure? = null,
    /** 解码后的视频画面尺寸；0 表示尚未就绪，用于把覆盖层限制在真实画面内。 */
    val videoWidth: Int = 0, val videoHeight: Int = 0,
)
data class PlaybackSource(
    val videoUrl: String, val audioUrl: String? = null,
    val headers: Map<String, String> = emptyMap(), val userAgent: String,
    val resumePositionMs: Long = 0, val autoPlay: Boolean = true, val speed: Float = 1f,
    val mediaKey: String? = null,
    /** 投屏优先用能带声音的渐进流（durl）；没有时才退回 [videoUrl]。 */
    val castUrl: String? = null,
    /** 非 null 时用本地 MPD 播放：换档只改轨道选择，不重建播放源，音频不会断。 */
    val dash: PlaybackDashSource? = null,
    val videoTarget: PlaybackVideoTarget? = null,
)

/** 前台、后台和系统媒体控件共享传输控制，不暴露 Media3 或 Android 类型。 */
interface PlaybackController {
    val state: StateFlow<PlaybackSnapshot>
    fun refresh(): PlaybackSnapshot
    /**
     * 只读当前播放位置：不读设置、不加锁、不发布状态。
     * 给弹幕时钟、常显进度条这类高频读取用，避免每秒几十次惊动整棵界面。
     */
    fun currentPositionMs(): Long = state.value.positionMs
    fun prepare(source: PlaybackSource)
    /**
     * 只换视频档位：当前已经是同一内容的 DASH 自适应源时，直接改轨道选择。
     *
     * @return true 表示已经在原位切换（播放位置/音频轨都没动）；false 表示调用方要按原方式重建播放源。
     */
    fun selectVideoQuality(qualityId: Int): Boolean = false
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun acknowledgeEnd()
    fun release()
    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    /** 当前视频直链；投屏需要把同一地址交给电视端。没有可投源时为 null。 */
    val currentMediaUrl: String? get() = null

    interface Listener {
        fun onRenderedFirstFrame() {}
        fun onPlayingChanged(playing: Boolean) {}
        fun onPhaseChanged(phase: PlaybackPhase) {}
        fun onError(error: PlaybackFailure) {}
    }
}
