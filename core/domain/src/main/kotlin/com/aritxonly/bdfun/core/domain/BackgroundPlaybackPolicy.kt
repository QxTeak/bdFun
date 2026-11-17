package com.aritxonly.bdfun.core.domain

/** 缓冲时 isPlaying=false，但用户仍希望续播，必须按播放意图保活。 */
fun shouldKeepPlaybackAlive(backgroundEnabled: Boolean, active: Boolean, playWhenReady: Boolean): Boolean =
    backgroundEnabled && active && playWhenReady
