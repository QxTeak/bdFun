package com.aritxonly.bdfun.core.domain

/** 缓冲时 isPlaying=false，但用户仍希望续播，必须按播放意图保活。 */
fun shouldKeepPlaybackAlive(backgroundEnabled: Boolean, active: Boolean, playWhenReady: Boolean): Boolean =
    backgroundEnabled && active && playWhenReady

/** 页面退出偏好只约束真正关闭页面，不能把失去可见性当作退出。 */
fun shouldPauseForVideoPageExit(stopOnExit: Boolean, finishing: Boolean, changingConfiguration: Boolean): Boolean =
    stopOnExit && finishing && !changingConfiguration
