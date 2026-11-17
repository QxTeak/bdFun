package com.aritxonly.bdfun.infra.playback

import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.common.C
import com.aritxonly.bdfun.core.domain.buffering.BufferInput
import com.aritxonly.bdfun.core.domain.buffering.BufferSession
import com.aritxonly.bdfun.core.domain.buffering.VideoBufferProvider

/** 只控制加载许可；Media3 保留音视频同步、数据请求与 allocator 生命周期。 */
internal class StrategyLoadControl(private val session: BufferSession, private val diagnosticLog: ((String) -> Unit)? = null) : DefaultLoadControl(
    DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
    // 即使覆盖加载决策，父类仍要求最小缓冲不小于卡顿恢复门槛。
    3_000, 120_000, 1_000, 3_000, VideoBufferProvider.MAX_BUFFER_BYTES, false, 0, false,
) {
    private var lastDiagnosticMs = 0L
    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean {
        val bytes = allocator.totalBytesAllocated
        val decision = session.decide(BufferInput(parameters.bufferedDurationUs / 1_000,
            parameters.playbackSpeed, bytes, parameters.playWhenReady))
        if (diagnosticLog != null) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastDiagnosticMs >= 5_000) {
                lastDiagnosticMs = now
                diagnosticLog.invoke("load allowed=${decision.shouldLoad} bufferedMs=${parameters.bufferedDurationUs / 1_000} " +
                    "lowMs=${decision.lowWaterMediaMs} targetMs=${decision.targetMediaMs} allocatedBytes=$bytes")
            }
        }
        return decision.shouldLoad
    }

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean {
        val speed = parameters.playbackSpeed.coerceAtLeast(0.1f)
        val bufferedWallMs = parameters.bufferedDurationUs / 1_000 / speed
        // 达到字节预算也须允许播放，避免高码率下永远等不到时间门槛。
        return bufferedWallMs >= (if (parameters.rebuffering) 3_000 else 1_000) ||
            allocator.totalBytesAllocated >= VideoBufferProvider.MAX_BUFFER_BYTES
    }
}
