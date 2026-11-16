package com.bytedance.danmaku.render.engine.render.layer.scroll

/**
 * 顶部让位只决定「新弹幕还能不能排进这一行」，不参与行的定位。
 *
 * 计数出现/消失时只改这个值：已经在屏上的弹幕继续按原位置画完，
 * 命中让位区的行不再接收新弹幕；计数消失后同一批行原地恢复可用，
 * 不会因为重新排布把旧弹幕顶走，也不会出现两行叠到同一个 y。
 */
internal fun isLineReservedByTopGuard(lineY: Float, reservedTopPx: Float): Boolean =
    reservedTopPx > 0f && lineY + 0.5f < reservedTopPx
