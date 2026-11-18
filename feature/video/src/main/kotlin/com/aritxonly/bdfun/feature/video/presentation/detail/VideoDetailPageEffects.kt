package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.aritxonly.bdfun.core.model.video.Quality
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem

@Composable
fun RegisterVideoDanmakuSender(
    detail: VideoDetail?, currentCid: Long,
    metadataController: com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder,
    detailController: com.aritxonly.bdfun.feature.video.application.VideoDetailStateHolder,
    ports: VideoDetailEffectPorts,
) {
    val VideoPageBus = ports.danmaku
    // 本页发送器共享给采集层外的Activity覆盖层，仍随原aid/cid绑定和释放。
    DisposableEffect(currentCid, detail?.aid) {
        val d = detail
        if (d != null) {
            VideoPageBus.danmakuLiker = { id -> metadataController.likeDanmaku(currentCid, id) }
            VideoPageBus.aid = d.aid
            VideoPageBus.cid = currentCid
            VideoPageBus.danmakuSender = { text, mode, fontSize, color ->
                val ok = metadataController.sendDanmaku(
                    com.aritxonly.bdfun.core.domain.DanmakuPost(
                        currentCid, d.bvid, text, VideoPageBus.danmakuProgressMs, mode, fontSize, color,
                    ),
                )
                if (ok) {
                    ports.sentHistory().record(
                        kind = "danmaku",
                        text = text,
                        bvid = d.bvid,
                        title = d.title,
                        videoTimeMs = VideoPageBus.danmakuProgressMs,
                    )
                    // 自己刚发的弹幕立即出现在弹幕层里（带圆角底）
                    detailController.appendDanmaku(com.aritxonly.bdfun.core.model.video.DanmakuItem(
                        timeMs = VideoPageBus.danmakuProgressMs,
                        mode = mode,
                        color = color,
                        size = fontSize / 25f,
                        text = text,
                        self = true,
                    ))
                }
                ok
            }
        }
        onDispose { VideoPageBus.danmakuSender = null; VideoPageBus.danmakuLiker = null }
    }

}

@Composable
fun BindBackgroundVideoMetadata(video: VideoItem, detail: VideoDetail?, currentCid: Long, activeQuality: Quality?, ports: VideoDetailEffectPorts) {
    // 底栏唱片用：当前播放的视频、标题、封面、集数。切 P / 切合集时一起更新。
    LaunchedEffect(
        video.bvid,
        detail?.bvid,
        detail?.title,
        detail?.cover,
        currentCid,
        activeQuality?.id,
    ) {
        val d = detail
        val item = video.copy(
            bvid = d?.bvid ?: video.bvid,
            aid = d?.aid ?: video.aid,
            title = d?.title ?: video.title,
            cover = d?.cover ?: video.cover,
            ownerName = d?.ownerName ?: video.ownerName,
            ownerFace = d?.ownerFace ?: video.ownerFace,
            ownerMid = d?.ownerMid ?: video.ownerMid,
            play = d?.play ?: video.play,
            danmaku = d?.danmaku ?: video.danmaku,
            duration = d?.duration ?: video.duration,
            pubdate = d?.pubdate ?: video.pubdate,
            extras = video.extras.copy(
                cid = currentCid,
            ),
        )
        ports.updateBackground(item, d, currentCid, activeQuality?.id ?: 0)
    }
}
