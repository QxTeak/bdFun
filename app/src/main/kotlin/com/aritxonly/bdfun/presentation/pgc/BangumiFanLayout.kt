package com.aritxonly.bdfun.presentation.pgc

import kotlin.math.abs

/**
 * 番剧页顶部扇形卡片的几何规则（纯 Kotlin，方便单测）。
 *
 * 5 个可见槽位是 -2..+2，第 6 张卡停在 +3 的隐藏槽等下一轮入场。
 * 槽位在 -2.5/+3.5 处环绕，而卡片从 2.0 就淡出、2.45 已完全透明，
 * 所以环绕发生在看不见的时候；6 张卡就能同时支持向前和向后切换，不用复制卡片。
 */
internal const val BANGUMI_FAN_SLOT_COUNT = 6

/** 可见槽位半径：|slot| <= 2 时卡片完整可见。 */
internal const val BANGUMI_FAN_VISIBLE_RADIUS = 2f

/** 每个槽位绕竖轴向内收的角度（度）。两侧像折屏一样朝中间倒，不是平面里的上下倾斜。 */
internal const val BANGUMI_FAN_ANGLE_PER_SLOT = 14f

/** 每个槽位缩小的比例；两侧逐级退后，中间那张保持最大。 */
internal const val BANGUMI_FAN_SCALE_STEP = 0.12f

/** 开始淡出的槽位距离；到 [BANGUMI_FAN_FADE_END] 完全透明。 */
internal const val BANGUMI_FAN_FADE_START = 2f
internal const val BANGUMI_FAN_FADE_END = 2.45f

/**
 * 卡片 [index] 相对连续焦点 [focus] 的槽位。
 *
 * focus 为整数时返回 -2..+3 的整数槽位；focus 连续变化时槽位平滑移动，
 * 越过边界 -2.5/+3.5 后环绕到另一端（此时卡片已经透明，看不到跳变）。
 */
internal fun bangumiFanSlot(index: Int, focus: Float, count: Int): Float {
    require(count > 0) { "fan count must be positive" }
    val raw = (index - focus + 2.5f) % count
    val wrapped = if (raw < 0f) raw + count else raw
    return wrapped - 2.5f
}

/** 绕竖轴的倾斜角：左侧正、右侧负，让每张卡的内侧边朝屏幕里收。 */
internal fun bangumiFanRotation(slot: Float): Float = -slot * BANGUMI_FAN_ANGLE_PER_SLOT

/** 槽位缩放，越靠外越小。 */
internal fun bangumiFanScale(slot: Float): Float =
    (1f - abs(slot) * BANGUMI_FAN_SCALE_STEP).coerceAtLeast(0.5f)
/** 槽位不透明度：可见区内保持 1，隐藏缓冲区里渐隐。 */
internal fun bangumiFanAlpha(slot: Float): Float {
    val distance = abs(slot)
    if (distance <= BANGUMI_FAN_FADE_START) return 1f
    if (distance >= BANGUMI_FAN_FADE_END) return 0f
    val t = (distance - BANGUMI_FAN_FADE_START) / (BANGUMI_FAN_FADE_END - BANGUMI_FAN_FADE_START)
    return 1f - t * t * (3f - 2f * t)
}
