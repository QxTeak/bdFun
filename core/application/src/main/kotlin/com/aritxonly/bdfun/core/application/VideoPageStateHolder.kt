package com.aritxonly.bdfun.core.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VideoPageState(
    val identity: VideoPageIdentity = VideoPageIdentity(),
    val requests: VideoPageRequests = VideoPageRequests(),
    val player: VideoPagePlayerState = VideoPagePlayerState(),
    val reply: VideoPageReplyState = VideoPageReplyState(),
) {
    val aid get() = identity.aid
    val cid get() = identity.cid
    val currentBvid get() = identity.currentBvid
    val autoNextRequest get() = requests.autoNextRequest
    val danmakuOn get() = player.danmakuOn
    val playerLiftPx get() = player.playerLiftPx
    val playerBottomPx get() = player.playerBottomPx
    val danmakuInputClose get() = requests.danmakuInputClose
    val danmakuProgressMs get() = player.danmakuProgressMs
    val replyEditing get() = reply.replyEditing
    val requestReplyFocus get() = requests.requestReplyFocus
    val consumedReplyFocus get() = requests.consumedReplyFocus
    val dismissReply get() = requests.dismissReply
    val consumedDismissReply get() = requests.consumedDismissReply
    val commentThreadVisible get() = reply.commentThreadVisible
    val fullscreen get() = player.fullscreen
    val controlsVisible get() = player.controlsVisible
    val orientationLocked get() = player.orientationLocked
    val replyName get() = reply.replyName
    val replyMid get() = reply.replyMid
    val replyRoot get() = reply.replyRoot
    val replyParent get() = reply.replyParent
}

data class VideoPageIdentity(
    val aid: Long = 0L,
    val cid: Long = 0L,
    val currentBvid: String = "",
)

data class VideoPageRequests(
    val autoNextRequest: Int = 0,
    val danmakuInputClose: Int = 0,
    val requestReplyFocus: Int = 0,
    val consumedReplyFocus: Int = 0,
    val dismissReply: Int = 0,
    val consumedDismissReply: Int = 0,
)

data class VideoPagePlayerState(
    val danmakuOn: Boolean = true,
    val playerLiftPx: Float = 0f,
    val playerBottomPx: Float = 0f,
    val danmakuProgressMs: Long = 0L,
    val fullscreen: Boolean = false,
    val controlsVisible: Boolean = false,
    val orientationLocked: Boolean = false,
)

data class VideoPageReplyState(
    val replyEditing: Boolean = false,
    val commentThreadVisible: Boolean = false,
    val replyName: String? = null,
    /** 被回复人的 mid：回复楼里某人时用来登记真正的 @ 提及。 */
    val replyMid: Long = 0L,
    val replyRoot: Long = 0L,
    val replyParent: Long = 0L,
)


/** 每个详情页独占一份跨层状态，页面叠开时不覆盖旧页面的浮层与回复目标。 */
class VideoPageStateHolder {
    /** 明细页共享的分段弹幕窗口；由 AppContainer 装配，未登录/预览页可以为 null。 */
    var danmakuFeed: DanmakuSegmentFeed? = null
    private val mutableState = MutableStateFlow(VideoPageState())
    val state = mutableState.asStateFlow()
    fun update(transform: (VideoPageState) -> VideoPageState) { mutableState.update(transform) }
}
