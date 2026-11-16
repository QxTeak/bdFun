package com.aritxonly.bdfun.core.domain

const val DEFAULT_FEED_REFRESH_RATE = 60

fun normalizeFeedRefreshRate(value: Int): Int = when (value) {
    0, 60, 120 -> value
    else -> DEFAULT_FEED_REFRESH_RATE
}
