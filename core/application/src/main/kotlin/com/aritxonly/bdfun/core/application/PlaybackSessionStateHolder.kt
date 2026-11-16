package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlaybackSessionState(
    val fullscreen: Boolean = false, val active: Boolean = false, val detached: Boolean = false, val handOffRequested: Boolean = false,
    val video: VideoItem? = null, val title: String = "", val author: String = "", val cover: String = "",
    val playback: PlaybackSnapshot = PlaybackSnapshot(),
)

/** 唯一的进程播放会话；旧页面退场不能清掉新页面，交接前后的进度来自同一控制器。 */
class PlaybackSessionStateHolder(private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(PlaybackSessionState())
    val state = mutableState.asStateFlow()
    private var owner: Any? = null
    private var binding: Job? = null
    private var revision = 0L
    var controller: PlaybackController? = null
        private set

    fun attach(owner: Any, controller: PlaybackController) {
        this.owner = owner
        bind(controller)
        mutableState.update { it.copy(active = true, detached = false, fullscreen = false, handOffRequested = false) }
    }

    private fun bind(controller: PlaybackController) {
        if (this.controller === controller) return
        // 新视频接管时立即停住旧传输，不能仅依赖窗口生命周期或音频焦点。
        this.controller?.pause()
        this.controller = controller
        binding?.cancel()
        val version = ++revision
        mutableState.update { it.copy(playback = controller.state.value) }
        binding = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            controller.state.collect { value ->
                if (version == revision) mutableState.update { it.copy(playback = value) }
            }
        }
    }

    /** true 表示页面仍是前台所有者且不交接，可以清理它注册的动作。 */
    fun detach(owner: Any): Boolean {
        if (this.owner !== owner) return false
        this.owner = null
        if (state.value.detached || state.value.handOffRequested) return false
        clear()
        return true
    }

    fun setFullscreen(owner: Any, fullscreen: Boolean) {
        if (this.owner === owner) mutableState.update { it.copy(fullscreen = fullscreen) }
    }
    fun requestHandOff() { mutableState.update { it.copy(handOffRequested = true) } }
    fun cancelHandOff() { mutableState.update { it.copy(handOffRequested = false) } }
    fun adopt(controller: PlaybackController) {
        bind(controller)
        mutableState.update { it.copy(active = true, detached = true, handOffRequested = false) }
    }
    fun updateMetadata(video: VideoItem?, title: String, author: String, cover: String) {
        mutableState.update { it.copy(video = video, title = title, author = author, cover = cover) }
    }
    fun clear() {
        owner = null
        revision++
        binding?.cancel(); binding = null
        controller = null
        mutableState.value = PlaybackSessionState()
    }
}
