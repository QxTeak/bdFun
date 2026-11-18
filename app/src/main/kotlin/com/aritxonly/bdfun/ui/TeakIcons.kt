package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import com.aritxonly.bdfun.R

/**
 * 保留 B 站原生语义素材；已选中的通用图标转发到 TeakUI 共享矢量。
 * 只暴露 drawable 契约，让播放器与输入框继续沿用原尺寸、按压和 tint。
 */
internal object TeakIcon {
    val Dislike = R.drawable.act_dislike
    val Coin = com.aritxonly.teak.common.icons.LucideIcons.CoinsFilled
    val Star = R.drawable.act_star
    val Share = R.drawable.act_share

    val CommentBubble = R.drawable.comment_bubble
    fun like(liked: Boolean): Int = if (liked) R.drawable.comment_like_filled else R.drawable.comment_like
    val CommentDislike = R.drawable.comment_dislike
    val CommentDislikeFilled = R.drawable.comment_dislike_filled

    val MetricPlay = R.drawable.ic_metric_play
    val MetricDanmaku = R.drawable.ic_metric_danmaku

    val Fullscreen = R.drawable.ic_fullscreen
    val Play = R.drawable.fs_play
    val SeekThumb = R.drawable.ic_seek_thumb

    // 用户为本轮全屏播放器提供的图标（黑底白图已转透明 PNG）
    val PlayerPlay = R.drawable.fs_play
    val PlayerDanmakuOn = R.drawable.fs_danmaku_on
    val PlayerDanmakuOff = R.drawable.fs_danmaku_off
    val PlayerDanmakuSettings = R.drawable.fs_danmaku_settings
    val PlayerNext = R.drawable.fs_next
    val PlayerComment = R.drawable.fs_comment
    val PlayerStar = R.drawable.fs_star
    val PlayerShare = R.drawable.fs_share

    val Send = com.aritxonly.teak.common.icons.LucideIcons.Send
}
