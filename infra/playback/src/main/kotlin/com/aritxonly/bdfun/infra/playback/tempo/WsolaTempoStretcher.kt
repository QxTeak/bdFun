package com.aritxonly.bdfun.infra.playback.tempo

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * WSOLA（波形相似重叠相加）变速器：改播放速度、不动音高。
 *
 * 为什么不用 ExoPlayer 内置的 Sonic：
 * - Sonic 只吃 PCM16，重叠相加在 `short` 上做，满幅素材容易在 grain 边界削波；
 * - Sonic 的搜索窗和交叉淡化都比 mpv 的 scaletempo2 粗糙，2x 以上金属感明显；
 * - Sonic 每次改速都会被 `flush()` 重建。
 *
 * 这里按 mpv / ffmpeg 那一类实现来做，但全程 float、状态可改速不重建：
 * - 每个输出 grain 只在开头与上一段尾巴做互相关对齐，再按互补权重交叉淡化；
 * - 输入位置用 Double 累计，长时间播放不会因为逐次取整产生音画漂移；
 * - 输出帧数严格等于 输入帧数 / speed（保留 double 精度）。
 *
 * 纯 Kotlin、无 Android 依赖，纯 JVM 单测可直接跑。
 */
internal class WsolaTempoStretcher(
    private val sampleRate: Int,
    private val channels: Int,
    frameMillis: Double = DEFAULT_FRAME_MILLIS,
    overlapMillis: Double = DEFAULT_OVERLAP_MILLIS,
    searchMillis: Double = DEFAULT_SEARCH_MILLIS,
) {
    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(channels > 0) { "channelCount must be positive" }
    }

    /** 播放速度。1.0 时调用方应整条旁路，这里只为完整性保留。 */
    var speed: Float = 1f
        set(value) {
            require(value > 0f) { "speed must be positive" }
            field = value.coerceIn(MIN_SPEED, MAX_SPEED)
        }

    private val frameFrames: Int = max(64, (sampleRate * frameMillis / 1000.0).roundToInt())

    // 交叉长度必须小于半个 grain，否则每次输出都落在淡化区里，声音会糊。
    private val overlapFrames: Int =
        max(8, min((sampleRate * overlapMillis / 1000.0).roundToInt(), frameFrames / 2 - 1))

    private val hopFrames: Int = frameFrames - overlapFrames
    private val searchFrames: Int = max(1, (sampleRate * searchMillis / 1000.0).roundToInt())

    /** 升余弦淡入权重；淡出取 1 - 淡入，两者相加恒为 1，不产生增益起伏。 */
    private val fadeIn = FloatArray(overlapFrames) { index ->
        (0.5 - 0.5 * cos(Math.PI * (index + 0.5) / overlapFrames)).toFloat()
    }

    /** 上一段输出的尾巴（interleaved），下一段开头要与它对齐并交叉淡化。 */
    private val tail = FloatArray(overlapFrames * channels)

    /** 第一段没有尾巴可接，直接原样输出，否则开头会有一段淡入。 */
    private var tailValid = false

    /** 尚未消费的输入（interleaved）。 */
    private var samples = FloatArray(0)

    /** [samples] 里有效的帧数。 */
    private var pendingFrames = 0

    /** [samples] 第 0 帧对应的绝对帧号。 */
    private var baseFrame = 0L

    /** 下一段 grain 的绝对起始帧，用 Double 累计避免取整漂移。 */
    private var readFrame = 0.0

    private val totalFrames: Long get() = baseFrame + pendingFrames

    /**
     * 追加输入并尽可能多地输出。
     *
     * [endOfStream] 为真时允许对末尾零填充，把最后一段 grain 冲出来。
     * 输出以 interleaved float 追加到 [out]，调用方自行清空。
     */
    fun process(
        input: FloatArray,
        offset: Int,
        frames: Int,
        endOfStream: Boolean,
        out: FloatAccumulator,
    ) {
        append(input, offset, frames)
        while (true) {
            val start = floor(readFrame).toLong()
            if (start >= totalFrames) break
            if (!endOfStream && start + frameFrames > totalFrames) break
            val remaining = (totalFrames - start).toInt()
            emitGrain(start, out, if (endOfStream) min(hopFrames, remaining) else hopFrames)
            readFrame += hopFrames.toDouble() * speed
            trimConsumed()
        }
    }

    /** 丢弃全部状态（seek / 换源 / 格式变化时由 AudioProcessor 调用）。 */
    fun flush() {
        pendingFrames = 0
        baseFrame = 0L
        readFrame = 0.0
        tail.fill(0f)
        tailValid = false
    }

    private fun append(input: FloatArray, offset: Int, frames: Int) {
        if (frames <= 0) return
        require(offset >= 0 && frames <= input.size / channels - offset) { "invalid input range" }
        val required = (pendingFrames + frames) * channels
        if (samples.size < required) {
            samples = samples.copyOf(max(required, max(samples.size * 2, 4096)))
        }
        System.arraycopy(
            input,
            offset * channels,
            samples,
            pendingFrames * channels,
            frames * channels,
        )
        pendingFrames += frames
    }

    private fun emitGrain(start: Long, out: FloatAccumulator, frames: Int) {
        val maxBack = if (tailValid) min(searchFrames.toLong(), start).toInt() else 0
        val bestOffset = findBestOffset(start, maxBack)
        val candidate = start + bestOffset

        val at = out.begin(frames * channels)
        val data = out.data
        for (index in 0 until frames) {
            if (tailValid && index < overlapFrames) {
                val weight = fadeIn[index]
                val inverse = 1f - weight
                for (channel in 0 until channels) {
                    val previous = tail[channel * overlapFrames + index]
                    val current = sample(candidate + index, channel)
                    data[at + index * channels + channel] = previous * inverse + current * weight
                }
            } else {
                for (channel in 0 until channels) {
                    data[at + index * channels + channel] = sample(candidate + index, channel)
                }
            }
        }

        // 下一段要淡入的，就是本段输出的最后 overlapFrames 帧（原样来自输入）。
        for (channel in 0 until channels) {
            val base = channel * overlapFrames
            for (index in 0 until overlapFrames) {
                tail[base + index] = sample(candidate + hopFrames + index, channel)
            }
        }
        tailValid = true
    }

    /**
     * 在 [-maxBack, searchFrames] 内找与 [tail] 最相似的候选起点。
     *
     * 先粗搜再细搜，最后对 0 偏移做一次「足够好就不动」的判定：
     * 周期性内容（音乐、长音）如果每次都换偏移，听感会颤。
     */
    private fun findBestOffset(start: Long, maxBack: Int): Int {
        val minOffset = -maxBack
        val maxOffset = searchFrames
        if (tailEnergy() < SILENCE_ENERGY) return 0

        val coarseStep = max(1, searchFrames / 16)
        var bestOffset = 0
        var bestScore = score(start, 0)

        var offset = minOffset
        while (offset <= maxOffset) {
            val current = score(start, offset)
            if (current > bestScore) {
                bestScore = current
                bestOffset = offset
            }
            offset += coarseStep
        }

        val from = max(minOffset, bestOffset - coarseStep)
        val until = min(maxOffset, bestOffset + coarseStep)
        offset = from
        while (offset <= until) {
            val current = score(start, offset)
            if (current > bestScore) {
                bestScore = current
                bestOffset = offset
            }
            offset++
        }

        if (bestOffset != 0 && score(start, 0) >= bestScore * ZERO_OFFSET_TOLERANCE) {
            return 0
        }
        return bestOffset
    }

    /** 归一化互相关：投影越大且候选能量越集中越好。 */
    private fun score(start: Long, offset: Int): Float {
        val candidate = start + offset
        var dot = 0.0
        var energy = SILENCE_ENERGY.toDouble()
        for (channel in 0 until channels) {
            val tailBase = channel * overlapFrames
            for (index in 0 until overlapFrames) {
                val a = tail[tailBase + index]
                val b = sample(candidate + index, channel)
                dot += (a * b).toDouble()
                energy += (b * b).toDouble()
            }
        }
        return (dot / sqrt(energy)).toFloat()
    }

    private fun tailEnergy(): Float {
        var energy = 0f
        for (value in tail) energy += value * value
        return energy
    }

    /** 越界读零：流末尾允许零填充，流开头由 [findBestOffset] 的 maxBack 挡住。 */
    private fun sample(frame: Long, channel: Int): Float {
        val index = frame - baseFrame
        if (index < 0 || index >= pendingFrames) return 0f
        return samples[(index.toInt() * channels) + channel]
    }

    private fun trimConsumed() {
        val keepFrom = max(0L, floor(readFrame).toLong() - searchFrames)
        val drop = (keepFrom - baseFrame).coerceIn(0L, pendingFrames.toLong()).toInt()
        if (drop <= 0) return
        val remaining = pendingFrames - drop
        if (remaining > 0) {
            System.arraycopy(samples, drop * channels, samples, 0, remaining * channels)
        }
        pendingFrames = remaining
        baseFrame += drop
    }

    private companion object {
        const val DEFAULT_FRAME_MILLIS = 40.0
        const val DEFAULT_OVERLAP_MILLIS = 12.0
        const val DEFAULT_SEARCH_MILLIS = 10.0
        const val MIN_SPEED = 0.1f
        const val MAX_SPEED = 8f
        const val SILENCE_ENERGY = 1e-6f
        const val ZERO_OFFSET_TOLERANCE = 0.98f
    }
}

/** 可增长的 interleaved float 缓冲。 */
internal class FloatAccumulator(initialCapacity: Int = 8192) {
    var data: FloatArray = FloatArray(initialCapacity)
        private set
    var size: Int = 0
        private set

    /** 预留 [extra] 个 float，返回写入起点并推进 [size]。 */
    fun begin(extra: Int): Int {
        ensure(size + extra)
        val start = size
        size += extra
        return start
    }

    fun append(source: FloatArray, offset: Int, count: Int) {
        if (count <= 0) return
        val start = begin(count)
        System.arraycopy(source, offset, data, start, count)
    }

    fun clear() {
        size = 0
    }

    private fun ensure(required: Int) {
        if (data.size >= required) return
        data = data.copyOf(max(required, data.size * 2))
    }
}
