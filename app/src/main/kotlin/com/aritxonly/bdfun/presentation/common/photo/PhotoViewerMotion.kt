package com.aritxonly.bdfun.ui

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 图片查看器的入场速度曲线。
 *
 * 参数直接沿用 HyperOS 4 搜索展开的 `FolmeEase.spring(0.97f, 0.4f)`：
 * 阻尼比 0.97、响应 0.4s（换算刚度 247），和 [com.aritxonly.teak.common.search.TeakSearchTransitionDefaults]
 * 是同一套非线性速率，而不是旧的 AccelerateDecelerate。
 *
 * 弹簧本身是无限衰减的，ValueAnimator 需要有限时长；这里把曲线按给定时长采样，
 * 再用该时刻的值归一化，保证末帧精确到 1，同时保留前段快、后段收的节奏。
 */
internal object PhotoViewerMotion {

    /** 与搜索展开一致的阻尼比。 */
    const val DampingRatio = 0.97f

    /** 由 response = 0.4s 换算出的刚度 ≈ (2π / 0.4)²。 */
    const val Stiffness = 247f

    /**
     * 打开动效时长：沿用搜索的 spring(0.97, 0.4s) 形状，但把实际播放窗口收到 320ms。
     * 弹簧前段已经走完大部分距离，320ms 的观感与 400ms 几乎一致，尾部不再多等 80ms。
     */
    const val OpenDurationMs = 320L

    /** 关闭沿用同一曲线但更短（response 0.3s），退出不该比展开还慢。 */
    const val CloseDurationMs = 300L

    /** 归一化后的 0..1 进度；[elapsedMs] 超过时长直接落到 1。 */
    fun progressAt(elapsedMs: Float, durationMs: Long): Float {
        if (elapsedMs <= 0f) return 0f
        if (durationMs <= 0L || elapsedMs >= durationMs) return 1f
        val end = valueAt(durationMs / 1000f)
        val endScale = if (end > 0.0001f) 1f / end else 1f
        return (valueAt(elapsedMs / 1000f) * endScale).coerceIn(0f, 1f)
    }

    /** 单位阶跃响应的解析解；t 单位为秒。 */
    private fun valueAt(seconds: Float): Float {
        val omega = sqrt(Stiffness)
        val zeta = DampingRatio
        val omegaD = omega * sqrt(1f - zeta * zeta)
        val decay = exp(-zeta * omega * seconds)
        return 1f - decay * (
            cos(omegaD * seconds) +
                (zeta * omega / omegaD) * sin(omegaD * seconds)
            )
    }
}
