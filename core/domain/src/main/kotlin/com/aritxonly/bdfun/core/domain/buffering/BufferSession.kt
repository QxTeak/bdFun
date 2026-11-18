package com.aritxonly.bdfun.core.domain.buffering

/** 时钟由平台注入；主线程采样与加载线程决策共享同一会话锁。 */
class BufferSession(private val store: BufferHistoryStore) {
    private var account = ""
    private var key: String? = null
    private var adaptive = false
    private var model = WatchHistory()
    private var provider = VideoBufferProvider(FixedCacheStrategy())
    private var watchedMs = 0L
    private var lastTick: Long? = null
    private var playing = false
    private var compromised = false
    private var ended = false
    private var rebuffered = false

    @Synchronized fun configure(owner: String, enabled: Boolean) {
        if (owner == account && enabled == adaptive) return
        // 开关和账号变化丢弃未提交样本，不能把旧会话归给新账号。
        account = owner
        adaptive = enabled
        watchedMs = 0
        lastTick = null
        compromised = true
        rebuffered = false
        model = if (enabled) store.read(owner) else WatchHistory()
        provider = VideoBufferProvider(if (enabled) AdaptiveCacheStrategy { model } else FixedCacheStrategy())
    }

    @Synchronized fun begin(mediaKey: String) {
        if (key == mediaKey) return
        finish()
        key = mediaKey
        watchedMs = 0
        lastTick = null
        playing = false
        compromised = false
        ended = false
        rebuffered = false
        if (adaptive) model = store.read(account)
        provider = VideoBufferProvider(if (adaptive) AdaptiveCacheStrategy { model } else FixedCacheStrategy())
    }

    @Synchronized fun sample(nowMs: Long, isPlaying: Boolean, buffering: Boolean, completed: Boolean) {
        val previous = lastTick
        if (previous != null && playing) watchedMs += (nowMs - previous).coerceAtLeast(0)
        lastTick = nowMs
        playing = isPlaying
        if (buffering && watchedMs > 0) {
            compromised = true
            rebuffered = true
        }
        ended = ended || completed
    }

    @Synchronized fun invalidate() { compromised = true }

    @Synchronized fun decide(input: BufferInput): BufferDecision =
        provider.decide(BufferContext(watchedMs, rebuffered), input)

    @Synchronized fun finish() {
        if (key != null && adaptive && watchedMs > 0) {
            store.observe(account, watchedMs, abandoned = !compromised && !ended)
        }
        key = null
        playing = false
        lastTick = null
    }
}
