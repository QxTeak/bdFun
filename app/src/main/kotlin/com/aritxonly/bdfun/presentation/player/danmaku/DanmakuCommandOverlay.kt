package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.domain.activeDanmakuCommand
import com.aritxonly.bdfun.navigateToVideoByBvid

/**
 * 视频互动：弹幕里的投票 / 评分 / 关注 / 跳转指令。
 *
 * 面板沿用「选中弹幕」那套黑色半透明玻璃（68% 黑 + 22% 白描边），不用系统 Card 的大色块；
 * 面板最大占播放器面积的一半。
 *
 * 位置/提交细节在 [DanmakuCommandHost] 与 [DanmakuCommandSession]；这里只负责
 * 按播放进度选出当前指令并接到页面上下文。
 */
@Composable
internal fun BoxScope.DanmakuCommandOverlay(
    playback: PlaybackController, aid: Long, cid: Long,
    actions: VideoInteractionActions, videoLiked: Boolean,
    authorFace: String = "", followed: Boolean = false,
    avatar: @Composable (String, androidx.compose.ui.Modifier) -> Unit = { _, _ -> },
) {
    val controller = LocalVideoPageState.current.danmakuModeration ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember(controller, aid, cid) {
        DanmakuCommandSession(controller, aid, cid, scope) { context.showToast(it) }
    }
    LaunchedEffect(session) { session.load() }
    val playbackState = playback.state.collectAsState()
    val active by remember(session.commands, session.dismissed, playbackState) {
        derivedStateOf {
            activeDanmakuCommand(session.commands, session.dismissed, playbackState.value.positionMs)
        }
    }
    active?.let { command ->
        val snapshot = playbackState.value
        DanmakuCommandHost(
            command = command,
            session = session,
            videoWidth = snapshot.videoWidth,
            videoHeight = snapshot.videoHeight,
            actions = actions,
            videoLiked = videoLiked,
            authorFace = authorFace,
            followed = followed,
            avatar = avatar,
        )
    }
    session.link?.let { target ->
        CommandLinkDialog(
            target = target,
            onDismiss = session::closeLink,
        ) { bvid ->
            session.closeLink()
            if (bvid.isBlank()) context.showToast("关联视频信息缺失")
            else navigateToVideoByBvid(context, bvid)
        }
    }
}
