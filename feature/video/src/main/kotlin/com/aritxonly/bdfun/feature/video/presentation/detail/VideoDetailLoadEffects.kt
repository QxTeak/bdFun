package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.delay
import com.aritxonly.bdfun.feature.video.application.VideoCacheEntry
import com.aritxonly.bdfun.feature.video.application.forPlaybackPart

@Composable
fun LoadInitialVideo(
    video: VideoItem, defaultQuality: Int, epId: Long, seasonId: Long,
    controllers: VideoDetailControllers, ui: VideoDetailUiStateHolder,
    ports: VideoDetailLoadPorts,
) {
    val settings = ports.settings
    val readCache = controllers.readCache
    val detailController = controllers.detail
    val selectionController = controllers.selection
    LaunchedEffect(video.bvid, epId, seasonId) {
        ui.metaReady.value = false
        // 离线缓存优先：直接从本地视频轨 + 音轨起播，不再请求播放地址。
        val offline = ports.downloads().findPlayable(video.bvid, video.cid)
        if (offline != null) {
            val offlinePlay = ports.downloads().playInfo(offline)
            ports.log(
                "VIDEO",
                "source=offline bvid=${video.bvid} qualities=${offlinePlay.qualities.size} " +
                    "audio=${offlinePlay.audioUrl != null}",
            )
            detailController.restore(ports.downloads().detail(offline), offlinePlay, emptyList(), emptyList())
            selectionController.restoreCid(offline.cid, video.cid)
            return@LaunchedEffect
        }
        // 从相关推荐点进去再返回：直接用缓存，不再重新请求
        val cached = readCache.video(video.bvid)?.takeIf { it.matchesPart(video.cid) }
        if (cached != null) {
            ports.log(
                "VIDEO",
                "source=memory-cache bvid=${video.bvid} " +
                    "qualities=${cached.playInfo?.qualities?.size} " +
                    "audio=${cached.playInfo?.audioUrl != null}",
            )
            detailController.restore(cached.detail, cached.playInfo, cached.danmaku,
                cached.related.ifEmpty { readCache.related(video.bvid).orEmpty() })
            val part = cached.detail.forPlaybackPart(video.cid)
            selectionController.restoreCid(part.cid, video.cid)
            if (settings.resumePlayback && selectionController.state.value.resumePositionMs <= 0L) {
                selectionController.offerCloudResume(detailController.resumePosition(part))
            }
            return@LaunchedEffect
        }
        val loadedSuccessfully = detailController.load(
            video.bvid, defaultQuality, settings.codecPreference, settings.cdnService,
            resume = { settings.resumePlayback && selectionController.state.value.resumePositionMs <= 0L },
            onPartSelected = { partCid ->
                // 只带 seasonId 进页面时初始 bvid 为空，详情回来后才把它补进页面标识。
                selectionController.restoreCid(partCid, video.cid,
                    detailController.state.value.detail?.bvid.orEmpty())
            },
            requestedCid = video.cid,
            onReady = { position ->
                selectionController.offerCloudResume(position)
                val ready = detailController.state.value
                ports.log(
                    "VIDEO",
                    "source=network bvid=${video.bvid} epId=$epId hasInfo=${ready.playInfo != null} " +
                        "qualities=${ready.playInfo?.qualities?.size} audio=${ready.playInfo?.audioUrl != null} " +
                        "err=${ready.playError}",
                )
            },
            epId = epId,
            seasonId = seasonId,
        )
        if (!loadedSuccessfully) return@LaunchedEffect
        val loaded = detailController.state.value
        loaded.detail?.let { d ->
            if (loaded.related.isNotEmpty() && d.bvid.isNotBlank()) readCache.putRelated(d.bvid, loaded.related)
            val part = d.forPlaybackPart(video.cid)
            val selected = selectionController.state.value
            if (d.bvid.isNotBlank() && selected.bvid == d.bvid && selected.cid == part.cid) {
                readCache.putVideo(d.bvid, VideoCacheEntry(d, loaded.playInfo, loaded.danmaku, loaded.related, part.cid))
            }
        }
    }

}

@Composable
fun BindVideoAuthor(
    detail: VideoDetail?, loggedIn: Boolean,
    metadataController: com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder,
    interactionController: com.aritxonly.bdfun.feature.video.application.VideoInteractionStateHolder,
    ui: VideoDetailUiStateHolder, description: VideoDescriptionStateHolder,
) {
    LaunchedEffect(detail?.bvid) {
        val d = detail ?: return@LaunchedEffect
        interactionController.bind(d)
        description.reset()
    }

    // 视频标签（话题/BGM/普通标签），接口与 PiliPlus 的 videoTags 相同。
    LaunchedEffect(detail?.bvid) {
        detail?.bvid?.takeIf { it.isNotBlank() }?.let { metadataController.loadTags(it) }
    }

    // UP 粉丝数 / 投稿数 / 当前关注状态
    LaunchedEffect(detail?.ownerMid) {
        val mid = detail?.ownerMid ?: 0L
        if (mid <= 0) return@LaunchedEffect
        val relationToken = interactionController.relationToken()
        metadataController.loadAuthor(mid, loggedIn, detail?.isFollowed == true) {
            interactionController.restoreFollowed(it, relationToken)
        }
        ui.metaReady.value = true
    }

    // 粉丝接口偶发很慢时，最多等一小会儿就让下方信息一起出现，避免页面长时间空着。
    LaunchedEffect(detail?.bvid) {
        if (detail == null) {
            ui.metaReady.value = false
            return@LaunchedEffect
        }
        delay(175)
        ui.metaReady.value = true
    }

}

@Composable
fun BindVideoPageMetadata(
    detail: VideoDetail?, currentCid: Long, loggedIn: Boolean,
    metadataController: com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder,
    interactionController: com.aritxonly.bdfun.feature.video.application.VideoInteractionStateHolder,
    ui: VideoDetailUiStateHolder,
) {
    // 当前同时观看人数
    LaunchedEffect(detail?.aid, currentCid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid <= 0 || currentCid <= 0) return@LaunchedEffect
        metadataController.loadOnline(d.aid, currentCid)
    }

    // 官方字幕轨：切分 P 后重新拉，默认关闭。
    LaunchedEffect(detail?.aid, currentCid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid > 0 && currentCid > 0) metadataController.loadSubtitles(d.aid, currentCid)
    }

    // 详情一到位就预取收藏夹，点「收藏」时直接出内容，不再转圈
    LaunchedEffect(detail?.aid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid <= 0 || !loggedIn) return@LaunchedEffect
        interactionController.bind(d)
        interactionController.loadFavorites()
    }

    // 预取失败时打开面板补齐，保持现有面板与提示方式。
    LaunchedEffect(ui.favSheet.value) {
        if (ui.favSheet.value && !interactionController.state.value.favoritesLoaded) interactionController.loadFavorites()
    }

}
