package com.aritxonly.bdfun.core.domain

/** 高刷屏已是主流：默认直接请求 120Hz，60Hz 设备由系统自行降级。 */
const val DEFAULT_FEED_REFRESH_RATE = 120

fun normalizeFeedRefreshRate(value: Int): Int = when (value) {
    0, 60, 120 -> value
    else -> DEFAULT_FEED_REFRESH_RATE
}
