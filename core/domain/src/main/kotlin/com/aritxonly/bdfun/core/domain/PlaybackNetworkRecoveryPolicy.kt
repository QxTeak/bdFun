package com.aritxonly.bdfun.core.domain

/** 只重试短暂断网/超时；鉴权、地址过期和解码失败继续交给原换源策略。 */
fun isTransientPlaybackNetworkFailure(code: String): Boolean = code in setOf(
    "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED", "ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT",
)

fun playbackNetworkRetryDelay(code: String, attempted: Int): Long? =
    if (isTransientPlaybackNetworkFailure(code)) when (attempted) {
        0 -> 1_000L
        1 -> 3_000L
        else -> null
    } else null
