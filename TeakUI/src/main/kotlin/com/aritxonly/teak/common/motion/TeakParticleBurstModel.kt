package com.aritxonly.teak.common.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** 粒子飘散的可调参数；改观感只动这里，不要散落到调用方。 */
object TeakParticleDefaults {
    /** 一簇爆炸最少 / 最多的碎屑数量。 */
    const val MinParticles = 18
    const val MaxParticles = 40

    /** 平均多少 dp² 面积一颗碎屑：被删项越大，炸得越满（整行卡片 ≈ 40 颗）。 */
    const val AreaPerParticleDp = 1100f

    /** 一次爆炸的总时长。屏幕上的飞行靠缓动曲线减速，不需要更长的线性时间。 */
    const val DurationMs = 700

    /** 剩余标签滑到新位置的时长，和卡片高度收缩的 320ms 对齐。 */
    const val BoundsMs = 320

    /** 碎屑从起点往外飞的距离，按被删项最长边的比例算：整行卡片和小胶囊观感一致。 */
    const val MinTravelFraction = 0.28f
    const val TravelFractionRange = 0.34f

    /** 碎屑边长范围（dp）。 */
    const val MinSizeDp = 1.8f
    const val SizeRangeDp = 2.6f

    /** 碎屑自转的总角度范围（度），正负各一半。 */
    const val SpinDegrees = 260f

    /** 起飞延迟占整段动画的比例，制造先后错落。 */
    const val MaxDelay = 0.10f

    /** 下坠加速度（dp/s² 量级）：只按本地时间平方叠加，不做真实物理积分。 */
    const val GravityDp = 26f
}

/**
 * 一次完整的爆炸，铺满被删项的 [bounds]（宿主坐标）后向外散开。
 *
 * 粒子参数在创建时按 [seed] 固定下来，重组或滚动回来时形状一致，不会每帧重新随机。
 * 起点在矩形内均匀分布、飞行方向沿「中心 → 起点」向外，所以整行视频卡片会整行炸开，
 * 而不是从中心点喷一朵小烟花。
 */
internal class TeakParticleBurst(
    private val bounds: Rect,
    color: Color,
    seed: Int,
    count: Int,
) {
    /** 0 → 1 的线性时间线；绘制时再按粒子自己的延迟换算成局部进度。 */
    val progress = Animatable(0f)

    private val particles: List<TeakParticle> = run {
        val random = Random(seed)
        // 同一份颜色的不同透明度：换色（主题强调色）会和「从这一项掉下来」对不上。
        val shades = listOf(color, color.copy(alpha = color.alpha * 0.82f), color.copy(alpha = color.alpha * 0.64f))
        val maxDimension = maxOf(bounds.width, bounds.height).coerceAtLeast(1f)
        val centerX = bounds.center.x
        val centerY = bounds.center.y
        List(count) {
            val x = bounds.left + random.nextFloat() * bounds.width
            val y = bounds.top + random.nextFloat() * bounds.height
            // 方向：从中心指向起点（中心附近的点随便给个方向），再抖动一点。
            val dx = x - centerX
            val dy = y - centerY
            val baseAngle = if (sqrt(dx * dx + dy * dy) < 1f) {
                random.nextFloat() * (2f * PI.toFloat())
            } else {
                atan2(dy, dx)
            }
            TeakParticle(
                start = Offset(x, y),
                angle = baseAngle + (random.nextFloat() - 0.5f) * 0.6f,
                travelPx = maxDimension * (TeakParticleDefaults.MinTravelFraction +
                    random.nextFloat() * TeakParticleDefaults.TravelFractionRange),
                sizeDp = TeakParticleDefaults.MinSizeDp +
                    random.nextFloat() * TeakParticleDefaults.SizeRangeDp,
                spin = (random.nextFloat() - 0.5f) * TeakParticleDefaults.SpinDegrees,
                delay = random.nextFloat() * TeakParticleDefaults.MaxDelay,
                color = shades[random.nextInt(shades.size)],
            )
        }
    }

    fun drawInto(scope: DrawScope) {
        val elapsed = progress.value
        if (elapsed >= 1f) return
        val density = scope.density
        particles.forEach { particle ->
            // 每颗粒子有自己的延迟，换算成 0..1 的局部时间。
            val local = ((elapsed - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            // 先快后慢：3 次方衰减比线性更像「炸开」而不是「匀速飘走」。
            val eased = 1f - (1f - local).pow(3)
            val distance = particle.travelPx * eased
            val fall = TeakParticleDefaults.GravityDp * density * local * local
            val x = particle.start.x + cos(particle.angle) * distance
            val y = particle.start.y + sin(particle.angle) * distance + fall
            val size = particle.sizeDp * density * (1f - local * 0.65f)
            val alpha = (1f - local).pow(1.35f)
            scope.rotate(degrees = particle.spin * local, pivot = Offset(x, y)) {
                scope.drawRoundRect(
                    // 乘上粒子自带的透明度层次，而不是覆盖它。
                    color = particle.color.copy(alpha = particle.color.alpha * alpha),
                    topLeft = Offset(x - size / 2f, y - size / 2f),
                    size = Size(size, size),
                    cornerRadius = CornerRadius(size * 0.28f),
                )
            }
        }
    }
}

/** 单颗碎屑的静态参数；只有颜色和几何，不含时间。 */
private class TeakParticle(
    val start: Offset,
    val angle: Float,
    val travelPx: Float,
    val sizeDp: Float,
    val spin: Float,
    val delay: Float,
    val color: Color,
)

internal fun DrawScope.drawTeakParticleBurst(burst: TeakParticleBurst) = burst.drawInto(this)
