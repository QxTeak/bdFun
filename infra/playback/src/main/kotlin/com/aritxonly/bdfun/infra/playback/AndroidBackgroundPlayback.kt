package com.aritxonly.bdfun.infra.playback

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.aritxonly.bdfun.core.application.ForegroundPlaybackAttachment
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackSource
import com.aritxonly.bdfun.core.model.video.VideoItemExtras
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 原生播放器/token、后台交接和释放线程归平台层，应用壳只提供配置与UI投影。 */
open class AndroidBackgroundPlayback(
    protected val session: com.aritxonly.bdfun.core.application.PlaybackSessionStateHolder,
    private val queue: com.aritxonly.bdfun.core.application.BackgroundQueueLoader,
    private val ports: BackgroundPlaybackPlatformPorts,
) : com.aritxonly.bdfun.core.application.BackgroundDiscPlayback, ForegroundPlaybackAttachment {

    override val state get() = session.state
    // app的Compose投影覆写这两个接缝，保持原读取与同步更新时机。
    protected open val snapshot get() = session.state.value
    protected open fun sync() {}
    val fullscreen get() = snapshot.fullscreen
    override fun updateFullscreen(owner: Any, value: Boolean) { session.setFullscreen(owner, value); sync() }
    val active get() = snapshot.active
    val detached get() = snapshot.detached
    override val playing get() = snapshot.playback.playing
    override val positionMs get() = snapshot.playback.positionMs
    override val durationMs get() = snapshot.playback.durationMs
    override var video: VideoItem?
        get() = snapshot.video
        set(value) { session.updateMetadata(value, title, author, cover); sync() }
    override var title: String
        get() = snapshot.title
        set(value) { session.updateMetadata(video, value, author, cover); sync() }
    override var author: String
        get() = snapshot.author
        set(value) { session.updateMetadata(video, title, value, cover); sync() }
    override var cover: String
        get() = snapshot.cover
        set(value) { session.updateMetadata(video, title, author, value); sync() }

    private var detail: VideoDetail? = null
    private var currentCid: Long = 0L
    private var qualityId: Int = 0

    private var token: Any? = null
    private var actions: ForegroundPlaybackAttachment.Actions? = null
    private var appContext: Context? = null

    private var detachedPlayer: PlaybackController? = null
    private var detachedScope: CoroutineScope? = null

    /**
     * 播放器释放单独放到后台线程：Media3 的 release() 会等待内部播放线程收尾，
     * 在部分机型/网络状态下会阻塞好几秒。放在主线程就会出现“上滑后整页顿住”。
     */
    private val teardownExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "bdFun-player-teardown").apply { isDaemon = true }
    }

    override fun attach(token: Any, playback: PlaybackController, actions: ForegroundPlaybackAttachment.Actions) {
        this.token = token
        this.actions = actions
        session.attach(token, playback)
        sync()
    }

    override fun detach(token: Any) {
        if (this.token !== token) return
        this.token = null
        if (detached || session.state.value.handOffRequested) {
            session.detach(token)
            actions = null
            return
        }
        session.detach(token)
        clearState()
    }

    fun updateInfo(video: VideoItem?, title: String, author: String, cover: String) {
        this.video = video
        this.title = title
        this.author = author
        this.cover = cover
    }

    fun updateSession(
        video: VideoItem?,
        detail: VideoDetail?,
        currentCid: Long,
        qualityId: Int,
    ) {
        if (video != null) this.video = video
        this.detail = detail
        this.currentCid = currentCid
        this.qualityId = qualityId
    }

    /** 视频页按返回时调用：先标记“要交棒”，随后正常 finish 让系统播一镜到底退出动画。 */
    fun requestHandOff() {
        session.requestHandOff(); sync()
    }

    fun cancelHandOff() {
        session.cancelHandOff(); sync()
    }

    fun shouldHandOff(): Boolean = session.state.value.handOffRequested

    /** 把仍在使用中的 ExoPlayer 交给后台。必须在 exo.release() 之前调用。 */
    fun adopt(exo: PlaybackController, context: Context) {
        if (detached) return
        session.adopt(exo); sync()
        session.cancelHandOff(); sync()
        actions = null
        appContext = context.applicationContext
        detachedPlayer = exo
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        detachedScope = scope
        scope.launch {
            while (isActive) {
                val playback = exo.refresh()
                if (ports.debug) {
                    ports.debugLog("BGP_STATE",
                        "id=${System.identityHashCode(exo)} position=${playback.positionMs} " +
                            "playing=${playback.playing} speed=${playback.speed}")
                }
                delay(500L)
            }
        }
        startKeepAlive(context)
    }

    fun startKeepAlive(context: Context) {
        appContext = context.applicationContext
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ports.serviceClass)
                    .setAction(ports.startAction),
            )
        }
    }

    fun stopKeepAlive() {
        if (detached) return
        val ctx = appContext ?: return
        runCatching { ctx.stopService(Intent(ctx, ports.serviceClass)) }
    }

    override fun next() {
        if (detached) {
            skipDetached(1)
        } else {
            actions?.next()
        }
    }

    override fun previous() {
        if (detached) {
            skipDetached(-1)
        } else {
            actions?.previous()
        }
    }

    override fun togglePlayPause() {
        val exo = detachedPlayer
        if (detached && exo != null) {
            if (exo.refresh().playing) exo.pause() else exo.play()
        } else {
            actions?.togglePlayPause()
        }
    }

    override fun boostStart() {
        if (detached) {
            detachedPlayer?.let { exo ->
                if (!exo.refresh().playing) return
                exo.setSpeed(ports.settings().boostDefaultSpeed)
            }
        } else {
            actions?.boostStart()
        }
    }

    override fun boostEnd() {
        if (detached) {
            detachedPlayer?.setSpeed(ports.settings().defaultPlaybackSpeed)
        } else {
            actions?.boostEnd()
        }
    }

    /** 唱片上旋转手指调进度。 */
    fun seekTo(ms: Long) {
        val max = durationMs.coerceAtLeast(0L)
        val target = if (max > 0L) ms.coerceIn(0L, max) else ms.coerceAtLeast(0L)
        if (detached) {
            detachedPlayer?.seekTo(target)
        } else {
            actions?.seekTo(target)
        }
    }

    /** 上滑退出：停掉后台播放器和常驻通知。 */
    override fun exit() {
        ports.trace(
            "BGP",
            "exit() detached=$detached actions=${actions != null}",
        )
        if (detached) {
            stopDetached()
        } else {
            actions?.exit()
            clearState()
        }
        ports.trace("BGP", "exit() done")
    }

    /** 打开新视频或单击唱片前调用：释放后台播放器，避免两路声音。 */
    fun stopDetached() {
        if (!detached) return
        val t0 = SystemClock.elapsedRealtime()
        val exo = detachedPlayer
        val scope = detachedScope
        val ctx = appContext
        // 先只做不阻塞的事：把界面状态清掉，重活全部交给后台线程。
        detachedPlayer = null
        detachedScope = null
        session.cancelHandOff(); sync()
        clearState()
        ports.trace(
            "BGP",
            "stopDetached: state cleared in ${SystemClock.elapsedRealtime() - t0}ms",
        )
        // 暂停放在主线程且只调用 pause()：它是往播放线程投消息，不会阻塞，
        // 能保证声音立刻停；其余 stop / release / 停服务 / 取消轮询全部放后台。
        runCatching { exo?.pause() }
        ports.trace(
            "BGP",
            "stopDetached: pause returned in ${SystemClock.elapsedRealtime() - t0}ms",
        )
        teardownExecutor.execute {
            val t1 = SystemClock.elapsedRealtime()
            scope?.cancel()
            runCatching { exo?.pause() }
            ports.trace(
                "BGP",
                "teardown: stop done in ${SystemClock.elapsedRealtime() - t1}ms",
            )
            runCatching { exo?.release() }
            ports.trace(
                "BGP",
                "teardown: release done in ${SystemClock.elapsedRealtime() - t1}ms",
            )
            if (ctx != null) {
                runCatching { ctx.stopService(Intent(ctx, ports.serviceClass)) }
            }
            ports.trace(
                "BGP",
                "teardown: all done in ${SystemClock.elapsedRealtime() - t1}ms",
            )
        }
    }

    private fun clearState() {
        queue.invalidate()
        token = null
        actions = null
        session.clear()
        sync()
        stopKeepAlive()
        detail = null
        currentCid = 0L
        qualityId = 0
    }

    private fun skipDetached(delta: Int) {
        val exo = detachedPlayer ?: return
        val d = detail ?: return
        val scope = detachedScope ?: return
        scope.launch {
            val track = queue.skip(d, currentCid, delta, qualityId,
                ports.settings().codecPreference, ports.settings().cdnService) ?: return@launch
            if (detachedPlayer !== exo || !detached) return@launch
            exo.prepare(PlaybackSource(
                videoUrl = track.audioUrl, userAgent = ports.userAgent,
                headers = mapOf("Referer" to "https://www.bilibili.com/video/${track.detail.bvid}/"),
            ))
            detail = track.detail
            currentCid = track.cid
            title = track.detail.title
            author = track.detail.ownerName
            cover = track.detail.cover
            video = track.detail.toVideoItem(currentCid)
        }
    }

    private fun VideoDetail.toVideoItem(cid: Long = this.cid): VideoItem = VideoItem(
        bvid = bvid,
        aid = aid,
        title = title,
        cover = cover,
        ownerName = ownerName,
        ownerFace = ownerFace,
        ownerMid = ownerMid,
        play = play,
        danmaku = danmaku,
        duration = duration,
        pubdate = pubdate,
        extras = VideoItemExtras(
            cid = cid,
            startPositionMs = positionMs,
        ),
    )
}
