package com.aritxonly.bdfun.infra.playback

import android.support.v4.media.session.PlaybackStateCompat
import com.aritxonly.bdfun.core.application.PlaybackPhase
import com.aritxonly.bdfun.core.application.PlaybackSnapshot

/** 缓冲与用户暂停是不同状态，系统会话必须保留这个区别。 */
internal fun mediaSessionPlaybackState(snapshot: PlaybackSnapshot): Int = when {
    snapshot.error != null -> PlaybackStateCompat.STATE_ERROR
    snapshot.phase == PlaybackPhase.Ended -> PlaybackStateCompat.STATE_STOPPED
    !snapshot.playWhenReady -> PlaybackStateCompat.STATE_PAUSED
    snapshot.phase == PlaybackPhase.Buffering -> PlaybackStateCompat.STATE_BUFFERING
    snapshot.playing -> PlaybackStateCompat.STATE_PLAYING
    else -> PlaybackStateCompat.STATE_PAUSED
}
