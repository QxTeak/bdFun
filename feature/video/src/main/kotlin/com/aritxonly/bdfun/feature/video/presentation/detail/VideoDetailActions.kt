package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.domain.VideoStreamRequest
import com.aritxonly.bdfun.core.model.video.Quality
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.model.video.VideoPage
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope


/** 页面动作只接线提示、系统入口和已有应用层控制器，不持有业务状态副本。 */
class VideoDetailViewActions(
    private val host: VideoDetailActionHost, private val scope: kotlinx.coroutines.CoroutineScope,
    private val video: VideoItem, controllers: VideoDetailControllers, private val ui: VideoDetailUiStateHolder,
    private val loggedIn: () -> Boolean,
    private val openedFromVideoPage: Boolean, private val onBack: () -> Unit,
) {
    private val settings get() = host.settings
    private val readCache = controllers.readCache
    private val detailController = controllers.detail
    private val interactionController = controllers.interaction
    private val selectionController = controllers.selection
    private val detail get() = detailController.state.value.detail
    val allowTwoCoins: Boolean get() = detail?.copyright != 2
    private val playInfo get() = detailController.state.value.playInfo
    private val currentCid get() = selectionController.state.value.cid
    private val qualityId get() = selectionController.state.value.quality
    private val activeBvid get() = selectionController.state.value.bvid
    private val favorited get() = interactionController.state.value.favorited
    private var coinSheet: Boolean
        get() = ui.coinSheet.value
        set(value) { ui.coinSheet.value = value }
    private var favSheet: Boolean
        get() = ui.favSheet.value
        set(value) { ui.favSheet.value = value }
    fun ensureLogin(): Boolean {
        if (!loggedIn()) {
            host.showToast("请先登录")
            return false
        }
        return true
    }

    /** 视图层只负责登录提示、面板开关与操作结果提示。 */
    fun ensureFavoritesLoaded() {
        if (!ensureLogin()) return
        scope.launch { interactionController.loadFavorites() }
    }

    fun performInteraction(action: suspend () -> String?) {
        if (!ensureLogin()) return
        scope.launch { action()?.let { host.showToast(it) } }
    }

    fun addWatchLater() = performInteraction { interactionController.watchLater() }
    fun followFromDanmaku() {
        if (!interactionController.state.value.followed) toggleFollow()
    }

    fun toggleFollow() {
        val mid = detail?.ownerMid ?: video.ownerMid
        if (mid > 0) performInteraction { interactionController.toggleFollow(mid) }
    }
    fun toggleLike() = performInteraction { interactionController.toggleLike() }
    fun toggleDislike() = performInteraction { interactionController.toggleDislike() }
    fun giveCoin() {
        if (detail != null && ensureLogin()) coinSheet = true
    }
    fun doCoin(count: Int, alsoLike: Boolean = false) {
        coinSheet = false
        performInteraction { interactionController.coin(count, alsoLike) }
    }
    fun toggleFavorite() {
        if (detail == null || !ensureLogin()) return
        if (!favorited) favSheet = true
        else performInteraction { interactionController.removeFavorites() }
    }
    fun tripleAction() = performInteraction { interactionController.tripleAction() }
    fun toggleFolder(folderId: Long) = performInteraction { interactionController.toggleFolder(folderId) }

    fun switchQuality(id: Int) {
        val d = detail ?: return
        if (!selectionController.selectQuality(id, host.progressMs)) return
        if (id == 0) return
        scope.launch {
            detailController.switchStream(
                VideoStreamRequest(d, id, currentCid, d.duration,
                    settings.codecPreference, settings.cdnService),
                reloadDanmaku = false,
            )
        }
    }

    /** 切到多 P 视频的某一集：换 cid、重置进度、重新拉播放流。 */
    fun switchPage(page: com.aritxonly.bdfun.core.model.video.VideoPage) {
        val d = detail ?: return
        if (page.cid == currentCid) return
        // 合集里的跨 bvid 单集：在当前播放器内切换，不跳新 Activity
        val episode = d.episodes.firstOrNull { it.cid == page.cid }
        if (d.isPgc && episode != null && episode.epId > 0L) {
            // 番剧/影视必须按 ep_id 重取详情：评论挂在 ep_id 上，播放地址也要带它。
            scope.launch {
                detailController.switchPgcEpisode(
                    episode.epId,
                    VideoStreamRequest(d, qualityId, page.cid, page.duration,
                        settings.codecPreference, settings.cdnService),
                ) { loaded ->
                    selectionController.selectPage(page.cid, loaded.bvid)
                    host.progressMs = 0L
                }
            }
            return
        }
        if (episode != null && episode.bvid.isNotBlank() && episode.bvid != d.bvid) {
            scope.launch {
                val switched = detailController.switchEpisode(
                    episode.bvid,
                    VideoStreamRequest(d, qualityId, page.cid, page.duration,
                        settings.codecPreference, settings.cdnService),
                ) { loaded ->
                    selectionController.selectPage(page.cid, loaded.bvid)
                    host.progressMs = 0L
                }
                if (switched) {
                    val state = detailController.state.value
                    if (state.related.isNotEmpty()) readCache.putRelated(episode.bvid, state.related)
                }
            }
            return
        }
        selectionController.selectPage(page.cid)
        host.progressMs = 0L
        scope.launch {
            detailController.switchStream(
                VideoStreamRequest(d, qualityId, page.cid, page.duration,
                    settings.codecPreference, settings.cdnService),
                reloadDanmaku = true,
            )
        }
    }

    /** 缓存当前视频当前 P 的指定清晰度（视频轨 + 音轨）。 */
    fun cacheVideo(quality: Quality) {
        val d = detail ?: return
        if (host.downloads.findPlayable(activeBvid, currentCid) != null) {
            host.showToast("已缓存")
            return
        }
        val audio = playInfo?.audioUrl
        if (audio.isNullOrBlank()) {
            host.showToast("该清晰度暂时无法缓存")
            return
        }
        val seconds = playInfo?.durationMs?.div(1000L)?.toInt()?.takeIf { it > 0 }
            ?: d.duration
        val queued = host.downloads.enqueue(
            detail = d,
            quality = quality,
            cid = currentCid,
            audioUrl = audio,
            durationSec = seconds,
        )
        host.showToast(if (queued) "已加入缓存" else "该清晰度已在缓存中")
    }

    /** 选集缓存的目标：多 P 的某一 P，或番剧/合集的某一集。 */
    private data class CacheTarget(
        val bvid: String, val cid: Long, val durationSec: Int, val epId: Long, val title: String,
    )

    private fun cacheTargets(d: VideoDetail): List<CacheTarget> {
        // 多 P 视频按分 P；番剧/影视/UGC 合集按单集（epId 只有 PGC 才有值）。
        if (d.pages.size > 1) {
            return d.pages.map { CacheTarget(d.bvid, it.cid, it.duration, 0L, it.part) }
        }
        return d.episodes.map { CacheTarget(it.bvid, it.cid, it.duration, it.epId, it.title) }
    }

    /**
     * 选集缓存：把当前视频的全部分 P / 合集单集按顺序加入下载队列。
     * 每集都要单独解析播放地址（音频/视频 URL 不是全局共享的），失败或已在缓存的跳过。
     */
    fun cacheAll(quality: Quality) {
        val d = detail ?: return
        val targets = cacheTargets(d)
        if (targets.size <= 1) {
            cacheVideo(quality)
            return
        }
        host.showToast("正在加入 ${targets.size} 集缓存…")
        scope.launch {
            var queued = 0
            var skipped = 0
            for (target in targets) {
                if (host.downloads.findPlayable(target.bvid, target.cid) != null) {
                    skipped++
                    continue
                }
                val resolved = detailController.resolveCacheSource(
                    bvid = target.bvid, cid = target.cid, durationSec = target.durationSec,
                    epId = target.epId, qualityId = quality.id,
                    codecPreference = settings.codecPreference, cdnService = settings.cdnService,
                )
                if (resolved == null) {
                    skipped++
                    continue
                }
                val (episodeDetail, info) = resolved
                val cacheQuality = info.qualities.firstOrNull { it.id == quality.id }
                if (cacheQuality == null || info.audioUrl.isNullOrBlank()) {
                    skipped++
                    continue
                }
                val added = host.downloads.enqueue(
                    detail = episodeDetail,
                    quality = cacheQuality,
                    cid = target.cid,
                    audioUrl = info.audioUrl,
                    durationSec = target.durationSec,
                )
                if (added) queued++ else skipped++
            }
            host.showToast(
                if (skipped == 0) "已加入 $queued 集缓存"
                else "已加入 $queued 集，跳过 $skipped 集",
            )
        }
    }

    /** 下一集：先走完当前视频的分 P，再进入合集的下一集。 */
    fun nextEpisode() {
        val d = detail ?: return
        val pageIndex = d.pages.indexOfFirst { it.cid == currentCid }
        val next = if (pageIndex >= 0 && pageIndex < d.pages.lastIndex) {
            d.pages[pageIndex + 1]
        } else {
            val episodeIndex = d.episodes.indexOfFirst {
                it.cid == currentCid || it.bvid == activeBvid
            }
            if (episodeIndex >= 0 && episodeIndex < d.episodes.lastIndex) {
                val ep = d.episodes[episodeIndex + 1]
                com.aritxonly.bdfun.core.model.video.VideoPage(
                    cid = ep.cid,
                    page = episodeIndex + 2,
                    part = ep.title,
                    duration = ep.duration,
                )
            } else {
                null
            }
        }
        if (next == null) {
            host.showToast("已经是最后一集")
        } else {
            switchPage(next)
        }
    }

    fun shareVideo() {
        val d = detail
        val url = if (d?.isPgc == true) {
            "https://www.bilibili.com/bangumi/play/ep${d.epId}"
        } else {
            "https://www.bilibili.com/video/${d?.bvid ?: video.bvid}"
        }
        host.share("${d?.title ?: video.title} $url")
        interactionController.recordShare()
    }

    /** 退出播放器：开启后台播放时先交棒，再正常关闭页面，保留一镜到底退出动画。 */
    fun leaveVideo() {
        handOffIfNeeded()
        onBack()
    }

    /** 收起态的回首页键：同样先交棒，再由宿主把主界面拉回推荐页。 */
    fun openHome() {
        handOffIfNeeded()
        host.openHome()
    }

    private fun handOffIfNeeded() {
        // 从相关推荐进来、返回是回到上一个视频页时，不交棒给后台：
        // 上一页会自己续播，交棒只会造成两路声音。
        if (settings.showDiscAfterVideo && !settings.stopOnVideoPageExit && !openedFromVideoPage) {
            host.requestHandOff()
        }
    }

}

@Composable
fun rememberVideoDetailActions(
    isLoggedIn: () -> Boolean, request: VideoScreenRequest, holders: VideoScreenStateHolders,
    scope: CoroutineScope, onBack: () -> Unit, host: VideoDetailActionHost,
): VideoDetailViewActions {
    val latestOnBack by rememberUpdatedState(onBack)
    val controllers = holders.controllers
    return remember(request.video.bvid, controllers.detail, controllers.interaction, controllers.selection, scope) {
        VideoDetailViewActions(host, scope, request.video, controllers, holders.ui,
            isLoggedIn, request.openedFromVideoPage, { latestOnBack() })
    }
}
