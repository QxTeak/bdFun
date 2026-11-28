package com.aritxonly.bdfun.core.domain

/**
 * 播到结尾后播放器停在 Ended，`playWhenReady` 仍然是 true（ExoPlayer 不会自动复位）。
 * 于是有两个后果必须单独判：
 *
 * 1. Ended 状态下单发 play() 是空操作，想重播必须先回到起点（拖进度条能重播就是这个原因）；
 * 2. 拿 `playWhenReady` 当“正在播”会把「重播」误判成「暂停」，也会让切后台再回来变成自动重播。
 *
 * 这里只放判断，不碰任何播放器实现。
 */

/** 播完后再按播放键，必须先把进度拉回起点；返回 null 表示维持原位置。 */
fun playbackRestartPositionMs(ended: Boolean): Long? = if (ended) 0L else null

/** 播放意图是否真的在播：Ended 时 `playWhenReady` 仍为 true，但它已经停了。 */
fun playbackIntentIsPlaying(ended: Boolean, playWhenReady: Boolean): Boolean = playWhenReady && !ended
