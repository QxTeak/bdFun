package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import top.yukonga.miuix.kmp.utils.overScrollVertical
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.TeakNavigator
import com.aritxonly.bdfun.MainActivity
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.DanmakuItem
import com.aritxonly.bdfun.data.FavoriteFolder
import com.aritxonly.bdfun.data.PlayInfo
import com.aritxonly.bdfun.data.Quality
import com.aritxonly.bdfun.data.VideoCacheManager
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.data.VideoPage
import com.kyant.shapes.Capsule
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 全屏选集用：优先展示 UGC 合集，否则展示多 P 分集。 */
private fun VideoDetail.playerPages(): List<VideoPage> {
    val episodePages = episodes.mapIndexed { index, episode ->
        VideoPage(
            cid = episode.cid,
            page = index + 1,
            part = episode.title,
            duration = episode.duration,
        )
    }
    // 有分 P 时先列分 P，再接合集里的其它视频；去重避免同一集出现两次。
    if (pages.size > 1) {
        return (pages + episodePages).distinctBy { it.cid }
    }
    if (episodePages.size > 1) return episodePages
    return pages
}

@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)
@Composable
fun VideoScreen(
    api: BiliApi,
    video: VideoItem,
    paneLayout: VideoPaneLayout,
    defaultQuality: Int = 80,
    onOpenComments: (VideoDetail) -> Unit = {},
    onBack: () -> Unit,
    autoRotate: Boolean = true,
    /** 视频画面顶到状态栏下面（设置项，默认关闭）。 */
    immersiveStatusBar: Boolean = false,
    pictureInPicture: Boolean = false,
    /** 由 Activity 从窗口根 insets 取到的状态栏高度；页面内部读到的可能被消费成 0。 */
    statusBarTop: Dp = rememberStatusBarTop(),
    /** 这个页面是从上一个视频页的相关推荐进来的：返回时回到上一页，不把声音交给后台。 */
    openedFromVideoPage: Boolean = false,
    /** 从消息页进入时要定位的评论。 */
    initialCommentRoot: Long = 0L,
    initialCommentTarget: Long = 0L,
    /** 消息链接里带的播放时间：不依赖「记忆播放进度」开关。 */
    initialSeekMs: Long = 0L,
    /** 由 VideoActivity 创建并持有：tab 栏直接读它的 currentPage，天然双向同步。 */
    pager: androidx.compose.foundation.pager.PagerState =
        androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 }),
) {
    val colors = MiuixTheme.colorScheme
    val videoContext = LocalContext.current
    var detail by remember { mutableStateOf<VideoDetail?>(null) }
    var activeBvid by remember { mutableStateOf(video.bvid) }
    var playInfo by remember { mutableStateOf<PlayInfo?>(null) }
    var related by remember(video.bvid) {
        mutableStateOf(RelatedMemory[video.bvid].orEmpty())
    }
    // 只记录当前 bvid。真正退出视频页时由 VideoActivity 的 onDestroy 清理；
    // 从相关推荐进另一个视频再返回时，这里不会被清，相关推荐不重载。
    LaunchedEffect(activeBvid) {
        VideoPageBus.currentBvid = activeBvid
    }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var qualityId by remember { mutableIntStateOf(defaultQuality) }
    var autoQuality by remember { mutableStateOf(defaultQuality == 0) }
    var resumePositionMs by remember {
        val explicit = when {
            initialSeekMs > 0L -> initialSeekMs
            AppGraph.settings.resumePlayback -> video.startPositionMs
            else -> 0L
        }
        val local = if (explicit <= 0L && AppGraph.settings.resumePlayback) {
            com.aritxonly.bdfun.data.PlaybackProgressStore.resumeFor(video.bvid)
        } else {
            0L
        }
        mutableLongStateOf(if (explicit > 0L) explicit else local)
    }
    var fullscreen by remember { mutableStateOf(false) }
    var danmaku by remember { mutableStateOf<List<DanmakuItem>>(emptyList()) }
    var followed by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }

    // ---- 播放器随浏览收起：完全照搬 BiliPai 的共享位移模型 ----
    // offsetPx 是唯一的状态，播放器高度、缩放、页面内边距都由它推导。
    val collapseDensity = LocalDensity.current
    val screenWidthForPlayer = paneLayout.widthDp.dp
    val videoAspectHeight = screenWidthForPlayer * 9f / 16f
    val collapseState = rememberInlinePlayerCollapseState(activeBvid)
    // 「上滑收起播放器」设置已删除，固定按原来的「关闭」模式运行。
    val collapseMode = com.aritxonly.bdfun.data.PlayerCollapseMode.OFF
    // bdFun 的视频详情页固定 16:9 横屏画面；竖屏视频仍按横屏详情页处理。
    val isVerticalVideo = false
    val pausedForInlineLayout = !playing
    val commentThreadVisible = VideoPageBus.commentThreadVisible
    val hidePausedPlayerForComments = shouldHidePausedInlinePlayerForComments(
        selectedTabIndex = pager.currentPage,
        isPlaybackPaused = pausedForInlineLayout,
        isCommentThreadVisible = commentThreadVisible,
    )
    val collapseEnabled = !paneLayout.isSplit && shouldEnableInlinePortraitScrollTransform(
        collapseMode = collapseMode,
        selectedTabIndex = pager.currentPage,
        isVerticalVideo = isVerticalVideo,
        isPlaybackPaused = pausedForInlineLayout,
        isCommentThreadVisible = commentThreadVisible,
    )
    // 整块播放器区域（含状态栏）在展开时的下边界。
    val expandedTopInset = if (immersiveStatusBar) 0.dp else statusBarTop
    val expandedPlayerHeight = videoAspectHeight + expandedTopInset
    // 最小时保留「状态栏 + 56dp 立即播放条」。
    val collapsedViewportHeightDp = resolveInlinePlayerCollapsedViewportHeightDp(
        standardCollapsedHeightDp = 56f,
        hidePausedPlayerForComments = hidePausedPlayerForComments,
        retainPlayToolbar = true,
    ).dp
    val collapsedPlayerHeight = statusBarTop + collapsedViewportHeightDp
    val collapseRangePx = with(collapseDensity) {
        (expandedPlayerHeight - collapsedPlayerHeight).coerceAtLeast(0.dp).toPx()
    }
    // 切页/暂停让可收起范围变化时，把超界位移平滑收回。
    LaunchedEffect(collapseEnabled, collapseRangePx) {
        collapseState.reconcileLayout(
            collapseEnabled = collapseEnabled,
            collapseRangePx = collapseRangePx,
        )
    }
    // 收起浏览时自动暂停，展开后只恢复本功能触发的暂停。
    var autoPausedByCollapse by remember(activeBvid) { mutableStateOf(false) }
    LaunchedEffect(playing) {
        if (playing) autoPausedByCollapse = false
    }
    val collapsedForAutoPause = resolvePlayerCollapsedForAutoPause(
        swipeHidePlayerEnabled = collapseEnabled,
        playerHeightOffsetPx = collapseState.offsetPx,
        videoHeightPx = collapseRangePx,
        compactForCommentTab = false,
        restoreRequested = collapseState.restoreRequested,
    )
    LaunchedEffect(collapsedForAutoPause, collapseEnabled) {
        when {
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = collapsedForAutoPause,
                isPlaying = playing,
            ) -> {
                autoPausedByCollapse = true
                VideoPageBus.pauseAll()
            }
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = collapsedForAutoPause,
                wasAutoPausedByCollapse = autoPausedByCollapse,
            ) -> {
                autoPausedByCollapse = false
                BackgroundPlayback.togglePlayPause()
            }
            !collapsedForAutoPause -> autoPausedByCollapse = false
        }
    }
    // 简介和评论共用同一个折叠手势；谁在滚动，都先收播放器，收满再把剩余距离交给列表。
    val collapseConnection = remember(collapseEnabled, collapseRangePx, collapseState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val update = reduceVideoDetailPreScroll(
                    currentOffsetPx = collapseState.offsetPx,
                    deltaPx = available.y,
                    // 切页后位移可能暂时超出新范围；先允许它从当前位置回到合法范围。
                    minOffsetPx = minOf(-collapseRangePx, collapseState.offsetPx),
                    inlinePortraitScrollEnabled = collapseEnabled,
                    isPortraitFullscreen = false,
                )
                return Offset(0f, collapseState.consumeScroll(update))
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val update = reduceVideoDetailPostScroll(
                    currentOffsetPx = collapseState.offsetPx,
                    deltaPx = available.y,
                    minOffsetPx = minOf(-collapseRangePx, collapseState.offsetPx),
                    inlinePortraitScrollEnabled = collapseEnabled,
                    isPortraitFullscreen = false,
                )
                return Offset(0f, collapseState.consumeScroll(update))
            }
        }
    }
    // 「展开回复」是独立 Dialog，拿不到这里的 nestedScroll，所以注册一个入口：
    // 它的列表往上划时，把同样的位移交给当前播放器，效果和主评论区一致。
    DisposableEffect(collapseConnection, collapseEnabled, collapseRangePx) {
        VideoPageBus.playerCollapseScroll = { deltaPx ->
            val update = if (deltaPx < 0f) {
                reduceVideoDetailPreScroll(
                    currentOffsetPx = collapseState.offsetPx,
                    deltaPx = deltaPx,
                    minOffsetPx = minOf(-collapseRangePx, collapseState.offsetPx),
                    inlinePortraitScrollEnabled = collapseEnabled,
                    isPortraitFullscreen = false,
                )
            } else {
                reduceVideoDetailPostScroll(
                    currentOffsetPx = collapseState.offsetPx,
                    deltaPx = deltaPx,
                    minOffsetPx = minOf(-collapseRangePx, collapseState.offsetPx),
                    inlinePortraitScrollEnabled = collapseEnabled,
                    isPortraitFullscreen = false,
                )
            }
            if (update == null) 0f else collapseState.consumeScroll(update)
        }
        onDispose { VideoPageBus.playerCollapseScroll = null }
    }
    // 高度只由共享位移推导：跟手拖动 snap，恢复展开/范围校正用 220ms 补间。
    val targetPlayerHeight = (
        expandedPlayerHeight + with(collapseDensity) { collapseState.offsetPx.toDp() }
        ).coerceIn(collapsedPlayerHeight, expandedPlayerHeight)
    val animatedPlayerHeight by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetPlayerHeight.value,
        animationSpec = if (collapseState.restoreRequested || collapseState.layoutReconciled) {
            androidx.compose.animation.core.tween(
                durationMillis = 220,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            )
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "inline_player_collapse_height",
    )
    val playerBlockHeight = animatedPlayerHeight.dp
    // 当前分 P / 合集的 cid（多 P 切换时只换流，不重建页面）
    var currentCid by remember { mutableLongStateOf(video.cid) }
    // 字幕：官方字幕轨 + 当前选择的字幕内容
    var subtitles by remember { mutableStateOf<List<com.aritxonly.bdfun.data.SubtitleTrack>>(emptyList()) }
    var selectedSubtitle by remember {
        mutableStateOf<com.aritxonly.bdfun.data.SubtitleTrack?>(null)
    }
    var subtitleCues by remember {
        mutableStateOf<List<com.aritxonly.bdfun.data.SubtitleCue>>(emptyList())
    }

    // 互动状态（初始值来自接口，点按后乐观更新再回写）
    var liked by remember { mutableStateOf(false) }
    var disliked by remember { mutableStateOf(false) }
    var likedCount by remember { mutableLongStateOf(0L) }
    var coined by remember { mutableStateOf(false) }
    var coinCount by remember { mutableLongStateOf(0L) }
    var favorited by remember { mutableStateOf(false) }
    var favoriteCount by remember { mutableLongStateOf(0L) }
    var shareCount by remember { mutableLongStateOf(0L) }
    var descExpanded by remember { mutableStateOf(false) }
    var descExpandable by remember { mutableStateOf(false) }
    var upFollowers by remember { mutableLongStateOf(0L) }
    var upVideos by remember { mutableLongStateOf(0L) }
    var onlineCount by remember { mutableLongStateOf(0L) }
    var onlineText by remember { mutableStateOf("") }
    // 播放器下方的信息统一等详情/粉丝数据回来后再一起淡入，避免一块一块跳出来。
    var metaReady by remember { mutableStateOf(false) }
    // 相关推荐整组共用一次入场：淡入 + 轻微上浮；懒加载后每一条都读同一个进度。
    val relatedEnter = rememberStaggerProgress(metaReady && related.isNotEmpty(), 9)
    // 相关推荐入场动画：只在这个视频页第一次出现时播；进出全屏不重播。
    // 投币 / 收藏夹 弹出面板
    var coinSheet by remember { mutableStateOf(false) }
    var favSheet by remember { mutableStateOf(false) }
    var folders by remember { mutableStateOf<List<com.aritxonly.bdfun.data.FavoriteFolder>>(emptyList()) }
    var folderIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var favLoading by remember { mutableStateOf(false) }
    var favPrefetched by remember { mutableStateOf(false) }
    val favoriteOperations = remember { com.aritxonly.bdfun.feature.library.application.FavoriteFolderOperations() }
    val pendingFavoriteFolder by favoriteOperations.pending.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(detail?.bvid) {
        val d = detail ?: return@LaunchedEffect
        liked = d.liked
        coined = d.coined
        favorited = d.favorited
        likedCount = d.like
        coinCount = d.coin
        favoriteCount = d.favorite
        shareCount = d.share
        descExpanded = false
        descExpandable = false
    }

    fun ensureLogin(): Boolean {
        if (!api.isLoggedIn) {
            context.showToast("请先登录")
            return false
        }
        return true
    }

    /** 全屏收藏面板打开时静默补齐收藏夹（预取没赶上时）。 */
    fun ensureFavoritesLoaded() {
        if (!api.isLoggedIn) {
            context.showToast("请先登录")
            return
        }
        if (favPrefetched || favLoading) return
        val d = detail ?: return
        favLoading = true
        scope.launch {
            runCatching { api.favoriteFolders() }.getOrNull()?.let { loaded ->
                folders = loaded
                folderIds = runCatching { api.favoriteFolderIds(d.aid) }.getOrDefault(emptySet())
                favPrefetched = true
            }
            favLoading = false
        }
    }

    /** 全屏更多菜单里的「稍后再看」。 */
    fun addWatchLater() {
        val d = detail ?: return
        if (!ensureLogin()) return
        scope.launch {
            val ok = api.addWatchLater(d.aid)
            context.showToast(if (ok) "已添加到稍后再看" else "添加稍后再看失败")
        }
    }

    fun syncInteractionCache() {
        val d = detail ?: return
        VideoCache.update(d.bvid) { cached ->
            cached.copy(
                liked = liked,
                like = likedCount,
                coined = coined,
                coin = coinCount,
                favorited = favorited,
                favorite = favoriteCount,
            )
        }
    }

    fun toggleFollow() {
        val mid = detail?.ownerMid ?: video.ownerMid
        if (mid <= 0) return
        if (!api.isLoggedIn) {
            context.showToast("请先登录")
            return
        }
        val next = !followed
        followed = next
        scope.launch {
            val result = runCatching { api.followUser(mid, next) }
            if (result.isSuccess) {
                context.showToast(if (next) "关注成功" else "已取消关注")
            } else {
                followed = !next
                context.showToast(result.exceptionOrNull()?.message ?: "操作失败")
            }
        }
    }

    fun toggleLike() {
        val d = detail ?: return
        if (!ensureLogin()) return
        val next = !liked
        // 服务端点赞会自动取消点踩，本地状态要同步清掉，避免两个图标同时高亮
        val wasDisliked = disliked
        val prevCount = likedCount
        liked = next
        likedCount = (likedCount + if (next) 1 else -1).coerceAtLeast(0)
        disliked = false
        syncInteractionCache()
        scope.launch {
            val result = runCatching { api.likeVideo(d.aid, next) }
            if (result.isSuccess) {
                context.showToast(if (next) "点赞成功" else "已取消赞")
            } else {
                liked = !next
                likedCount = prevCount
                disliked = wasDisliked
                syncInteractionCache()
                context.showToast(result.exceptionOrNull()?.message ?: "点赞失败")
            }
        }
    }

    fun toggleDislike() {
        val d = detail ?: return
        if (!ensureLogin()) return
        val next = !disliked
        // 点踩会取消点赞：计数要一起减回去
        val wasLiked = liked
        val prevLikeCount = likedCount
        disliked = next
        if (next && liked) {
            liked = false
            likedCount = (likedCount - 1).coerceAtLeast(0)
        }
        syncInteractionCache()
        scope.launch {
            val result = runCatching { api.dislikeVideo(d.aid, next) }
            if (result.isSuccess) {
                context.showToast(if (next) "感谢反馈" else "已取消反馈")
            } else {
                disliked = !next
                liked = wasLiked
                likedCount = prevLikeCount
                syncInteractionCache()
                context.showToast(result.exceptionOrNull()?.message ?: "操作失败")
            }
        }
    }

    fun giveCoin() {
        if (detail == null) return
        if (!ensureLogin()) return
        coinSheet = true
    }

    fun doCoin(count: Int) {
        val d = detail ?: return
        coinSheet = false
        scope.launch {
            val ok = runCatching { api.coinVideo(d.aid, count, false) }.isSuccess
            if (ok) {
                coined = true
                coinCount += count
                syncInteractionCache()
                context.showToast("投币成功")
            } else {
                context.showToast("投币失败")
            }
        }
    }

    fun toggleFavorite() {
        val d = detail ?: return
        if (!ensureLogin()) return
        if (!favorited) {
            // 未收藏：打开面板选收藏夹
            favSheet = true
            return
        }
        // 已收藏：直接取消，不再弹面板
        scope.launch {
            val ids = if (folderIds.isNotEmpty()) {
                folderIds
            } else {
                runCatching { api.favoriteFolderIds(d.aid) }.getOrDefault(emptySet())
            }
            if (ids.isEmpty()) {
                context.showToast("取消收藏失败")
                return@launch
            }
            var result = runCatching { api.removeFavoriteAll(d.aid, ids) }
            if (result.isFailure) {
                // 有的账号/收藏夹下批量删除会返回错误，退回逐个删除
                var anyOk = false
                ids.forEach { id ->
                    if (runCatching { api.setFavorite(d.aid, id, false) }.isSuccess) anyOk = true
                }
                if (anyOk) result = Result.success(true)
            }
            if (result.isSuccess) {
                folderIds = emptySet()
                favorited = false
                favoriteCount = (favoriteCount - 1).coerceAtLeast(0)
                syncInteractionCache()
                context.showToast("已取消收藏")
            } else {
                context.showToast(
                    "取消收藏失败：${result.exceptionOrNull()?.message ?: "未知错误"}",
                )
            }
        }
    }

    /** 一键三连：点赞 + 投 2 币 + 收藏（已经做过的项跳过，不会取消）。 */
    fun tripleAction() {
        val d = detail ?: return
        if (!ensureLogin()) return
        scope.launch {
            var ok = true
            if (!liked) {
                val r = runCatching { api.likeVideo(d.aid, true) }
                if (r.isSuccess) {
                    liked = true
                    likedCount += 1
                    disliked = false
                } else {
                    ok = false
                }
            }
            if (!coined) {
                val r = runCatching { api.coinVideo(d.aid, 2, false) }
                if (r.isSuccess) {
                    coined = true
                    coinCount += 2
                } else {
                    ok = false
                }
            }
            if (!favorited) {
                val folder = folders.firstOrNull()
                    ?: runCatching { api.favoriteFolders() }.getOrNull()?.firstOrNull()
                if (folder != null) {
                    val r = runCatching { api.setFavorite(d.aid, folder.id, true) }
                    if (r.isSuccess) {
                        favorited = true
                        folderIds = folderIds + folder.id
                        favoriteCount += 1
                    } else {
                        ok = false
                    }
                } else {
                    ok = false
                }
            }
            syncInteractionCache()
            context.showToast(if (ok) "已一键三连" else "一键三连部分失败")
        }
    }

    fun toggleFolder(folderId: Long) {
        val d = detail ?: return
        if (!favoriteOperations.tryStart(folderId)) return
        val contains = folderIds.contains(folderId)
        scope.launch {
            try {
                val ok = try { api.setFavorite(d.aid, folderId, !contains) }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { false }
                if (ok) {
                    folderIds = if (contains) folderIds - folderId else folderIds + folderId
                    favorited = folderIds.isNotEmpty()
                    favoriteCount = (favoriteCount + if (contains) -1 else 1).coerceAtLeast(0)
                    syncInteractionCache()
                    context.showToast(if (contains) "已取消收藏" else "已收藏")
                } else {
                    context.showToast(if (contains) "取消收藏失败" else "收藏失败")
                }
            } finally { favoriteOperations.finish(folderId) }
        }
    }

    fun switchQuality(id: Int) {
        if (id == 0) {
            // 自动：按当前网速从现在拿到的清晰度列表里重新挑一档，不需要重新请求播放流。
            if (autoQuality) return
            resumePositionMs = VideoPageBus.danmakuProgressMs
            autoQuality = true
            qualityId = 0
            return
        }
        autoQuality = false
        if (id == qualityId) return
        val d = detail ?: return
        resumePositionMs = VideoPageBus.danmakuProgressMs
        qualityId = id
        scope.launch {
            playInfo = runCatching {
                api.playInfo(
                    d,
                    id,
                    currentCid,
                    d.duration,
                    codecPreference = AppGraph.settings.codecPreference,
                    cdnService = AppGraph.settings.cdnService,
                )
            }.getOrNull()
        }
    }

    /** 切到多 P 视频的某一集：换 cid、重置进度、重新拉播放流。 */
    fun switchPage(page: com.aritxonly.bdfun.data.VideoPage) {
        val d = detail ?: return
        if (page.cid == currentCid) return
        // 合集里的跨 bvid 单集：在当前播放器内切换，不跳新 Activity
        val episode = d.episodes.firstOrNull { it.cid == page.cid }
        if (episode != null && episode.bvid.isNotBlank() && episode.bvid != d.bvid) {
            scope.launch {
                val loaded = runCatching { api.view(episode.bvid) }.getOrNull() ?: return@launch
                detail = loaded
                activeBvid = loaded.bvid
                currentCid = page.cid
                resumePositionMs = 0L
                VideoPageBus.danmakuProgressMs = 0L
                playInfo = runCatching {
                    api.playInfo(
                        loaded,
                        qualityId,
                        page.cid,
                        page.duration,
                        codecPreference = AppGraph.settings.codecPreference,
                        cdnService = AppGraph.settings.cdnService,
                    )
                }.getOrNull()
                danmaku = runCatching { api.danmaku(page.cid, page.duration) }
                    .getOrDefault(emptyList())
                related = runCatching { api.related(loaded.bvid) }
                    .getOrDefault(emptyList())
                    .also { if (it.isNotEmpty()) RelatedMemory[loaded.bvid] = it }
            }
            return
        }
        currentCid = page.cid
        resumePositionMs = 0L
        VideoPageBus.danmakuProgressMs = 0L
        scope.launch {
            playInfo = runCatching {
                api.playInfo(
                    d,
                    qualityId,
                    page.cid,
                    page.duration,
                    codecPreference = AppGraph.settings.codecPreference,
                    cdnService = AppGraph.settings.cdnService,
                )
            }.getOrNull()
            danmaku = runCatching { api.danmaku(page.cid, page.duration) }
                .getOrDefault(emptyList())
        }
    }

    /** 缓存当前视频当前 P 的指定清晰度（视频轨 + 音轨）。 */
    fun cacheVideo(quality: Quality) {
        val d = detail ?: return
        if (VideoCacheManager.findPlayable(activeBvid, currentCid) != null) {
            videoContext.showToast("已缓存")
            return
        }
        val audio = playInfo?.audioUrl
        if (audio.isNullOrBlank()) {
            videoContext.showToast("该清晰度暂时无法缓存")
            return
        }
        val seconds = playInfo?.durationMs?.div(1000L)?.toInt()?.takeIf { it > 0 }
            ?: d.duration
        val queued = VideoCacheManager.enqueue(
            detail = d,
            quality = quality,
            cid = currentCid,
            audioUrl = audio,
            durationSec = seconds,
        )
        videoContext.showToast(if (queued) "已加入缓存" else "该清晰度已在缓存中")
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
                com.aritxonly.bdfun.data.VideoPage(
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
            context.showToast("已经是最后一集")
        } else {
            switchPage(next)
        }
    }

    fun shareVideo() {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "${detail?.title ?: video.title} https://www.bilibili.com/video/${video.bvid}",
            )
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "分享视频"),
        )
        if (detail != null) shareCount += 1
    }

    /** 退出播放器：开启后台播放时先交棒，再正常关闭页面，保留一镜到底退出动画。 */
    fun leaveVideo() {
        // 从相关推荐进来、返回是回到上一个视频页时，不交棒给后台：
        // 上一页会自己续播，交棒只会造成两路声音。
        if (AppGraph.settings.showDiscAfterVideo && !AppGraph.settings.stopOnVideoPageExit && !openedFromVideoPage) {
            com.aritxonly.bdfun.ui.BackgroundPlayback.requestHandOff()
        }
        onBack()
    }

    BackHandler {
        when {
            fullscreen -> fullscreen = false
            else -> leaveVideo()
        }
    }

    LaunchedEffect(video.bvid) {
        metaReady = false
        // 离线缓存优先：直接从本地视频轨 + 音轨起播，不再请求播放地址。
        val offline = VideoCacheManager.findPlayable(video.bvid, video.cid)
        if (offline != null) {
            val offlinePlay = offline.toPlayInfo()
            com.aritxonly.bdfun.data.LogCollector.record(
                "VIDEO",
                "source=offline bvid=${video.bvid} qualities=${offlinePlay.qualities.size} " +
                    "audio=${offlinePlay.audioUrl != null}",
            )
            detail = offline.toDetail()
            playInfo = offlinePlay
            currentCid = offline.cid
            danmaku = emptyList()
            related = emptyList()
            loading = false
            return@LaunchedEffect
        }
        // 从「我的评论与弹幕」等入口进来时，先定位到记录的时间点
        if (AppGraph.settings.resumePlayback && video.startPositionMs > 0L) {
            resumePositionMs = video.startPositionMs
        }
        // 从相关推荐点进去再返回：直接用缓存，不再重新请求
        val cached = VideoCache.get(video.bvid)
        if (cached != null) {
            com.aritxonly.bdfun.data.LogCollector.record(
                "VIDEO",
                "source=memory-cache bvid=${video.bvid} " +
                    "qualities=${cached.playInfo?.qualities?.size} " +
                    "audio=${cached.playInfo?.audioUrl != null}",
            )
            detail = cached.detail
            playInfo = cached.playInfo
            danmaku = cached.danmaku
            related = cached.related.ifEmpty { RelatedMemory[video.bvid].orEmpty() }
            currentCid = cached.detail.cid
            if (AppGraph.settings.resumePlayback && resumePositionMs <= 0L) {
                resumePositionMs = runCatching {
                    api.cloudResumePositionMs(
                        cached.detail.aid,
                        cached.detail.cid,
                        cached.detail.duration,
                    )
                }.getOrDefault(0L)
            }
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val viewed = runCatching { api.view(video.bvid) }
        val loaded = viewed.getOrNull()
        if (loaded == null) {
            error = viewed.exceptionOrNull()?.message ?: "加载失败"
            loading = false
        } else {
            detail = loaded
            currentCid = loaded.cid
            // playurl / 弹幕 / 相关推荐互不依赖，并行发起：
            // 播放地址先回来就先起播，不再等弹幕和相关推荐串行跑完。
            var playError: Throwable? = null
            val playDeferred = async {
                runCatching {
                    api.playInfo(
                        loaded,
                        defaultQuality,
                        codecPreference = AppGraph.settings.codecPreference,
                        cdnService = AppGraph.settings.cdnService,
                    )
                }.onFailure { playError = it }.getOrNull()
            }
            val danmakuDeferred = async {
                runCatching { api.danmaku(loaded.cid, loaded.duration) }
                    .getOrDefault(emptyList())
            }
            val relatedDeferred = async {
                runCatching { api.related(video.bvid) }.getOrDefault(emptyList())
            }
            // 本地没有进度时，再问一次 B 站云端（换设备/重装后靠这个）。
            val cloudResumeDeferred = async {
                if (AppGraph.settings.resumePlayback && resumePositionMs <= 0L) {
                    runCatching {
                        api.cloudResumePositionMs(loaded.aid, loaded.cid, loaded.duration)
                    }.getOrDefault(0L)
                } else {
                    0L
                }
            }
            val play = playDeferred.await()
            val cloudResume = cloudResumeDeferred.await()
            if (resumePositionMs <= 0L && cloudResume > 0L) {
                resumePositionMs = cloudResume
            }
            playInfo = play
            com.aritxonly.bdfun.data.LogCollector.record(
                "VIDEO",
                "source=network bvid=${video.bvid} hasInfo=${play != null} " +
                    "qualities=${play?.qualities?.size} audio=${play?.audioUrl != null} " +
                    "err=${playError?.javaClass?.simpleName}:${playError?.message?.take(160)}",
            )
            // 播放地址就绪即可撤掉全屏 loading，弹幕和相关推荐在后台继续补。
            loading = false
            danmaku = danmakuDeferred.await()
            val fetchedRelated = relatedDeferred.await()
            if (fetchedRelated.isNotEmpty()) {
                related = fetchedRelated
                RelatedMemory[video.bvid] = fetchedRelated
            }
            VideoCache.put(
                video.bvid,
                VideoCacheEntry(loaded, playInfo, danmaku, related),
            )
        }
    }

    // UP 粉丝数 / 投稿数 / 当前关注状态
    LaunchedEffect(detail?.ownerMid) {
        val mid = detail?.ownerMid ?: 0L
        if (mid <= 0) return@LaunchedEffect
        // 缓存里的 isFollowed 可能是进视频那一刻的旧值，重新问一次官方关系接口
        val attribute = if (api.isLoggedIn) {
            runCatching { api.relation(mid) }.getOrNull()
        } else {
            null
        }
        followed = if (attribute != null) {
            attribute == 2 || attribute == 6
        } else {
            detail?.isFollowed == true
        }
        upFollowers = runCatching { api.relationStat(mid).second }.getOrDefault(0L)
        upVideos = api.upVideoCount(mid)
        metaReady = true
    }

    // 粉丝接口偶发很慢时，最多等一小会儿就让下方信息一起出现，避免页面长时间空着。
    LaunchedEffect(detail?.bvid) {
        if (detail == null) {
            metaReady = false
            return@LaunchedEffect
        }
        delay(175)
        metaReady = true
    }

    // 当前同时观看人数
    LaunchedEffect(detail?.aid, currentCid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid <= 0 || currentCid <= 0) return@LaunchedEffect
        val (count, text) = api.onlineTotal(d.aid, currentCid)
        onlineCount = count
        onlineText = text
    }

    // 官方字幕轨：切分 P 后重新拉，默认关闭
    LaunchedEffect(detail?.aid, currentCid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid <= 0 || currentCid <= 0) return@LaunchedEffect
        subtitles = runCatching { api.subtitles(d.aid, currentCid) }.getOrDefault(emptyList())
        selectedSubtitle = null
        subtitleCues = emptyList()
    }

    // 选中字幕轨后下载 JSON 并解析成时间轴
    LaunchedEffect(selectedSubtitle) {
        val track = selectedSubtitle ?: run {
            subtitleCues = emptyList()
            return@LaunchedEffect
        }
        subtitleCues = runCatching { api.subtitleCues(track.url) }.getOrDefault(emptyList())
    }

    // 详情一到位就预取收藏夹，点「收藏」时直接出内容，不再转圈
    LaunchedEffect(detail?.aid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid <= 0 || !api.isLoggedIn) return@LaunchedEffect
        val loaded = runCatching { api.favoriteFolders() }.getOrNull() ?: return@LaunchedEffect
        folders = loaded
        folderIds = runCatching { api.favoriteFolderIds(d.aid) }.getOrDefault(emptySet())
        favPrefetched = true
    }

    // 预取失败（或没来得及）时，打开面板只做静默补齐；面板本身不显示 loading
    LaunchedEffect(favSheet) {
        if (!favSheet || favPrefetched) return@LaunchedEffect
        val d = detail ?: return@LaunchedEffect
        favLoading = true
        runCatching { api.favoriteFolders() }.getOrNull()?.let { loaded ->
            folders = loaded
            folderIds = runCatching { api.favoriteFolderIds(d.aid) }.getOrDefault(emptySet())
            favPrefetched = true
        }
        favLoading = false
    }

    // 把本页的 aid/cid 与「发弹幕」发送器交给 Activity 覆盖层（那边在采集层之外，拿不到 api）
    DisposableEffect(currentCid, detail?.aid) {
        val d = detail
        if (d != null) {
            VideoPageBus.aid = d.aid
            VideoPageBus.cid = currentCid
            VideoPageBus.danmakuSender = { text, mode, fontSize, color ->
                val ok = runCatching {
                    api.sendDanmaku(
                        cid = currentCid,
                        bvid = d.bvid,
                        text = text,
                        progressMs = VideoPageBus.danmakuProgressMs,
                        mode = mode,
                        fontSize = fontSize,
                        color = color,
                    )
                }.isSuccess
                if (ok) {
                    AppGraph.sentHistory.record(
                        kind = "danmaku",
                        text = text,
                        bvid = d.bvid,
                        title = d.title,
                        videoTimeMs = VideoPageBus.danmakuProgressMs,
                    )
                    // 自己刚发的弹幕立即出现在弹幕层里（带圆角底）
                    danmaku = danmaku + com.aritxonly.bdfun.data.DanmakuItem(
                        timeMs = VideoPageBus.danmakuProgressMs,
                        mode = mode,
                        color = color,
                        size = fontSize / 25f,
                        text = text,
                        self = true,
                    )
                }
                ok
            }
        }
        onDispose { VideoPageBus.danmakuSender = null }
    }

    // 独占全屏状态共享给 Activity；横屏分栏保留 tab / 回复栏。
    LaunchedEffect(fullscreen) { PlayerUiState.fullscreen = fullscreen }
    // 独占全屏时收回投币 / 收藏弹层。
    LaunchedEffect(fullscreen) {
        if (fullscreen) {
            coinSheet = false
            favSheet = false
        }
    }
    // 横屏页面布局和播放器独占全屏分别管理。
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val landscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // Rotation opens the two-pane page. Fullscreen is a separate, explicit player action.
    // Rotating back to portrait still leaves fullscreen; entering it while portrait can rotate first.
    LaunchedEffect(landscape) {
        if (!landscape) fullscreen = false
    }
    val splitPanes = paneLayout.isSplit && !fullscreen && !pictureInPicture

    val qualities = playInfo?.qualities.orEmpty()
    val activeQuality = if (autoQuality) {
        autoQualityFor(qualities, videoContext)
    } else {
        qualities.firstOrNull { it.id == qualityId } ?: qualities.firstOrNull()
    }

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
            cid = currentCid,
        )
        com.aritxonly.bdfun.ui.BackgroundPlayback.updateInfo(
            video = item,
            title = item.title,
            author = item.ownerName,
            cover = item.cover,
        )
        com.aritxonly.bdfun.ui.BackgroundPlayback.updateSession(
            video = item,
            detail = d,
            currentCid = currentCid,
            qualityId = activeQuality?.id ?: 0,
        )
    }

    // 状态栏高度：隐藏状态栏时不变，首帧 insets 未下发时按系统资源兜底。
    // 视频容器和 tab 都用这一个值，进入页面时不会一个先跳一个后跳。
    val statusTop = statusBarTop

    // 兜底：极少数情况下 pager 停在上一次拖动的中间位置，
    // 松手后强制吸附到最近的一页，避免停在 0.3 / 0.7 这种位置。
    LaunchedEffect(pager) {
        snapshotFlow {
            pager.isScrollInProgress to pager.currentPageOffsetFraction
        }.collect { (scrolling, offset) ->
            if (!scrolling && kotlin.math.abs(offset) > 0.02f) {
                val target = (pager.currentPage + offset)
                    .roundToInt()
                    .coerceIn(0, pager.pageCount - 1)
                runCatching { pager.animateScrollToPage(target) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        if (pager.currentPage == 0) TrackTeakPageScroll(listState)

        // 全屏时只隐藏、不卸载：否则退回竖屏评论列表会整段重载
        run {
            // 全屏时强制沿用竖屏尺寸，别让 LazyColumn 在横屏里重新测量、
            // 把底部已经组合好的「相关推荐」扔掉，退出全屏后又重新淡入一遍。
            var portraitContentSize by remember {
                mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)
            }
            val density = LocalDensity.current
            val freezePortrait = (fullscreen || pictureInPicture) &&
                portraitContentSize.width > 0 && portraitContentSize.height > 0
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .then(
                        if (freezePortrait) {
                            with(density) {
                                Modifier.size(
                                    portraitContentSize.width.toDp(),
                                    portraitContentSize.height.toDp(),
                                )
                            }
                        } else if (splitPanes) {
                            Modifier.width(paneLayout.detailsWidthDp.dp).fillMaxHeight()
                                .background(colors.background)
                        } else {
                            Modifier.fillMaxSize()
                        },
                    )
                    .onSizeChanged {
                        if (!fullscreen && !pictureInPicture) portraitContentSize = it
                    }
                    .graphicsLayer { alpha = if (fullscreen || pictureInPicture) 0f else 1f }
                    .blockTouches(fullscreen || pictureInPicture),
            ) {
                HorizontalPager(
                    state = pager,
                    // 两页都保持 compose：切到简介再回来，评论列表不重载
                    beyondViewportPageCount = 1,
                    // 只有两页：到边后再往外滑不要任何回弹动画
                    overscrollEffect = null,
                    // 允许左右滑动；tab 直接读 pager.currentPage，会跟着一起动
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(collapseConnection),
                ) { page ->
                    if (page == 0) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = (if (splitPanes) statusBarTop else playerBlockHeight) + 52.dp,
                                bottom = 32.dp,
                            ),
                        ) {
            item(key = "meta") {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    // UP 行：头像 + 昵称 + 签名 + 粉色实心关注
                    StaggerLine(metaReady, 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = rememberImageRequest(detail?.ownerFace ?: video.ownerFace),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                ) {
                                    // 进个人主页前先停掉视频，不要带着声音过去
                                    VideoPageBus.pauseAll()
                                    TeakNavigator.openSpace(
                                        videoContext,
                                        detail?.ownerMid ?: video.ownerMid,
                                    )
                                }
                                .clip(CircleShape)
                                .background(colors.surfaceContainerHigh),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f).offset(y = 1.dp)) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    text = detail?.ownerName ?: video.ownerName,
                                    color = colors.onBackground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                )
                            }
                            Text(
                                text = when {
                                    upFollowers > 0 || upVideos > 0 ->
                                        "粉丝 ${formatCount(upFollowers)} · 视频 ${formatCount(upVideos)}"
                                    else -> detail?.ownerSign.orEmpty().ifBlank {
                                        detail?.tname.orEmpty().ifEmpty { "UP 主" }
                                    }
                                },
                                color = colors.onBackgroundVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.offset(x = 1.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        // 纯色 + 高光描边的关注按钮
                        Box(
                            modifier = Modifier
                                .clip(Capsule())
                                .background(BrandPink)
                                .border(1.dp, Color.White.copy(alpha = 0.45f), Capsule())
                                .clickable(interactionSource = null, indication = null) {
                                    toggleFollow()
                                }
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = if (followed) "已关注" else "+ 关注",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    }
                    }

                    Spacer(Modifier.height(10.dp))
                    // 标题：可长按选择 / 复制
                    StaggerLine(metaReady, 1) {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = detail?.title ?: video.title,
                            color = colors.onBackground,
                            fontSize = 17.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    }
                    Spacer(Modifier.height(4.dp))
                    // Keep each statistic atomic so narrow panes wrap whole items, not digits.
                    StaggerLine(metaReady, 2) {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MetricGlyph(play = true, color = colors.onBackgroundVariant, size = 14.dp)
                                Spacer(Modifier.width(3.dp))
                                Text(formatCount(detail?.play ?: video.play.toLong()),
                                    color = colors.onBackgroundVariant, fontSize = 12.sp, maxLines = 1)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MetricGlyph(play = false, color = colors.onBackgroundVariant, size = 14.dp)
                                Spacer(Modifier.width(3.dp))
                                Text(formatCount(detail?.danmaku ?: video.danmaku.toLong()),
                                    color = colors.onBackgroundVariant, fontSize = 12.sp, maxLines = 1)
                            }
                            detail?.pubdate?.takeIf { it > 0 }?.let {
                                Text(formatDateTime(it), color = colors.onBackgroundVariant,
                                    fontSize = 12.sp, maxLines = 1)
                            }
                            if (onlineCount > 0 || onlineText.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PersonGlyph(color = colors.onBackgroundVariant, size = 12.dp)
                                    Spacer(Modifier.width(3.dp))
                                    Text("${onlineText.ifBlank { formatCount(onlineCount) }} 人",
                                        color = colors.onBackgroundVariant, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                    val descText = detail?.desc?.takeIf { it.isNotBlank() }
                    val descProgress = rememberManualProgress(
                        if (descExpanded) 1f else 0f,
                        durationMs = 420f,
                    )
                    Spacer(Modifier.height(4.dp))
                    // BV 号那一栏：最右侧的箭头负责展开 / 收起简介（简介没被折叠时不显示）
                    StaggerLine(metaReady, 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = detail?.bvid ?: video.bvid,
                                color = colors.onBackgroundVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (descText != null && descExpandable) {
                            Box(
                                modifier = Modifier
                                    .size(width = 34.dp, height = 24.dp)
                                    .clip(Capsule())
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                    ) { descExpanded = !descExpanded },
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .size(width = 12.dp, height = 8.dp)
                                        .graphicsLayer { rotationZ = 180f * descProgress },
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(w * 0.12f, h * 0.28f)
                                        lineTo(w * 0.5f, h * 0.72f)
                                        lineTo(w * 0.88f, h * 0.28f)
                                    }
                                    drawPath(
                                        path = path,
                                        color = colors.onBackgroundVariant,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 1.6.dp.toPx(),
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    }
                    StaggerLine(metaReady, 4) {
                    if (descText != null) {
                        Spacer(Modifier.height(4.dp))
                        CollapsibleDescription(
                            text = descText,
                            progress = descProgress,
                            color = colors.onBackgroundVariant,
                            backgroundColor = colors.background,
                            onExpandableChange = { descExpandable = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 1.dp)
                                .pointerInput(descText) {
                                    detectTapGestures(
                                        onLongPress = {
                                            context.copyToClipboard("简介", descText)
                                            context.showToast("已复制简介")
                                        },
                                    )
                                },
                        )
                    }
                    }

                    // 分区标签
                    StaggerLine(metaReady, 5) {
                    if (!detail?.tname.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Box(
                                modifier = Modifier
                                    .clip(Capsule())
                                    .background(colors.surfaceContainerHigh)
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    text = detail!!.tname,
                                    color = colors.onBackgroundVariant,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                    }

                    Spacer(Modifier.height(14.dp))
                    // 五宫格：点赞 / 不喜欢 / 币 / 收藏 / 分享
                    StaggerLine(metaReady, 6) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        ActionStat(
                            modifier = Modifier.weight(1f),
                            label = "点赞",
                            value = likedCount,
                            kind = ActionKind.ThumbUp,
                            active = liked,
                            onClick = { toggleLike() },
                            onLongPress = { tripleAction() },
                        )
                        ActionStat(
                            modifier = Modifier.weight(1f),
                            label = "不喜欢",
                            value = 0,
                            kind = ActionKind.ThumbDown,
                            active = disliked,
                            onClick = { toggleDislike() },
                        )
                        ActionStat(
                            modifier = Modifier.weight(1f),
                            label = "投币",
                            value = coinCount,
                            kind = ActionKind.Coin,
                            active = coined,
                            onClick = { giveCoin() },
                        )
                        ActionStat(
                            modifier = Modifier.weight(1f),
                            label = "收藏",
                            value = favoriteCount,
                            kind = ActionKind.Star,
                            active = favorited,
                            onClick = { toggleFavorite() },
                        )
                        ActionStat(
                            modifier = Modifier.weight(1f),
                            label = "分享",
                            value = shareCount,
                            kind = ActionKind.Share,
                            active = false,
                            onClick = { shareVideo() },
                        )
                    }
                    }
                }
            }

            val detailForPages = detail
            if (detailForPages != null && detailForPages.pages.size > 1) {
                item(key = "pages-card") {
                    val enter = rememberStaggerProgress(metaReady, 7)
                    // 分 P 不要整体背景：每个 P 自己一张白色圆角卡片。
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                            .padding(vertical = 8.dp)
                            .graphicsLayer {
                                alpha = enter
                                translationY = (1f - enter) * 12.dp.toPx()
                            },
                    ) {
                        VideoPageStrip(
                            pages = detailForPages.pages,
                            currentCid = currentCid,
                            onSelect = { switchPage(it) },
                        )
                    }
                }
            }
            if (detailForPages != null && detailForPages.episodes.size > 1) {
                item(key = "collection-card") {
                    val enter = rememberStaggerProgress(metaReady, 8)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 10.dp)
                            .graphicsLayer {
                                alpha = enter
                                translationY = (1f - enter) * 12.dp.toPx()
                            },
                    ) {
                        VideoCollectionCard(
                            title = detailForPages.seasonTitle.ifBlank { detailForPages.title },
                            episodes = detailForPages.episodes,
                            currentCid = currentCid,
                            onSelect = { episode ->
                                val index = detailForPages.episodes
                                    .indexOf(episode)
                                    .coerceAtLeast(0)
                                switchPage(
                                    com.aritxonly.bdfun.data.VideoPage(
                                        cid = episode.cid,
                                        page = index + 1,
                                        part = episode.title,
                                        duration = episode.duration,
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            if (error != null) {
                item(key = "error") {
                    Text(
                        text = error!!,
                        modifier = Modifier.padding(16.dp),
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                    )
                }
            }

            if (related.isNotEmpty()) {
                itemsIndexed(
                    items = related,
                    key = { _, item -> "related-${item.bvid}" },
                ) { index, relatedItem ->
                    val isFirst = index == 0
                    val isLast = index == related.lastIndex
                    val shape = RoundedCornerShape(
                        topStart = if (isFirst) 16.dp else 0.dp,
                        topEnd = if (isFirst) 16.dp else 0.dp,
                        bottomStart = if (isLast) 16.dp else 0.dp,
                        bottomEnd = if (isLast) 16.dp else 0.dp,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 12.dp,
                                end = 12.dp,
                                top = if (isFirst) 14.dp else 0.dp,
                                bottom = if (isLast) 6.dp else 0.dp,
                            )
                            .graphicsLayer {
                                alpha = relatedEnter
                                translationY = (1f - relatedEnter) * 12.dp.toPx()
                            }
                            .clip(shape)
                            .background(colors.surface)
                            .padding(vertical = 2.dp),
                    ) {
                        RelatedRow(
                            item = relatedItem,
                            onClick = {
                                // 先同步停掉当前播放器，再启动新页面，避免转场期间双声。
                                // 从这个页面点相关推荐进去的页面，返回时是回到本页，
                                // 所以新页面退出时不要交给后台播放，否则会和本页叠音。
                                VideoPageBus.pauseAll()
                                com.aritxonly.bdfun.TeakNavigator.openVideo(
                                    context,
                                    relatedItem,
                                    fromVideoPage = true,
                                )
                            },
                        )
                    }
                }
            }
                        }
                    } else {
                        val d = detail
                        if (d != null) {
                            CommentScreen(
                                api = api,
                                aid = d.aid,
                                upMid = d.ownerMid,
                                title = d.title,
                                onBack = {},
                                bvid = d.bvid,
                                bare = true,
                                trackBackProgress = pager.currentPage == 1,
                                topInset = if (splitPanes) statusBarTop + 52.dp else playerBlockHeight + 14.dp +
                                    (if (immersiveStatusBar) 36.dp else 0.dp),
                                threadPlayerBottom = if (splitPanes) statusBarTop + 52.dp else playerBlockHeight,
                                threadPaneWidth = if (splitPanes) maxWidth else null,
                                playerFullscreen = fullscreen,
                                focusRootRpid = initialCommentRoot,
                                focusTargetRpid = initialCommentTarget,
                            )
                        }
                    }
                }
            }
        }

        if (loading) {
            TeakLoading(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
                    .size(28.dp),
                color = colors.primary,
            )
        }

        // 收起进度、顶部避让全部由共享位移推导（BiliPai 同款）。
        val collapseRangeDp = (expandedPlayerHeight - collapsedPlayerHeight).coerceAtLeast(0.dp)
        val collapseProgress = if (fullscreen || pictureInPicture || splitPanes || collapseRangeDp.value <= 0f) {
            0f
        } else {
            ((expandedPlayerHeight.value - animatedPlayerHeight) / collapseRangeDp.value)
                .coerceIn(0f, 1f)
        }
        // 沉浸模式下，收起过程中把画面顶部让到状态栏下面，避免被状态栏裁切。
        val playerTopInset = if (immersiveStatusBar) statusTop * collapseProgress else statusTop
        // 把播放器真实底边同步给 Activity：Tab 和渐变用它定位，永远贴着视频底边。
        val playerDensity = androidx.compose.ui.platform.LocalDensity.current
        androidx.compose.runtime.SideEffect {
            VideoPageBus.playerBottomPx = with(playerDensity) { playerBlockHeight.toPx() }
        }
        Box(
            modifier = if (fullscreen || pictureInPicture) {
                Modifier.fillMaxSize()
            } else if (splitPanes) {
                Modifier.align(Alignment.TopStart)
                    .width(paneLayout.playerWidthDp.dp).fillMaxHeight()
                    .background(Color.Black)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(top = expandedTopInset)
                    .clipToBounds()
            } else {
                Modifier
                    .fillMaxWidth()
                    // 黑色底就是播放器窗口本身：窗口收缩，画面跟着收缩，两者不会分离。
                    // 状态栏那一条也要铺黑，否则沉浸时视频缩下去会露出白色页面底。
                    .height(playerBlockHeight)
                    .background(androidx.compose.ui.graphics.Color.Black)
                    .padding(top = playerTopInset)
                    .clipToBounds()
                    .align(Alignment.TopCenter)
            },
        ) {
            // 画面直接铺满不断收缩的播放器窗口：BiliPai 的 continuousPlayerViewportHeight。
            // 窗口变矮时 PlayerView 会按高度贴合，视频底边始终贴住窗口底边（Tab 行）。
            VideoPlayer(
                bvid = activeBvid,
                info = playInfo,
                quality = activeQuality,
                autoQuality = autoQuality,
                cover = detail?.cover ?: video.cover,
                fullscreen = fullscreen,
                title = detail?.title ?: video.title,
                author = detail?.ownerName ?: video.ownerName,
                danmaku = danmaku,
                fullscreenInfo = FullscreenPlayerInfo(
                    title = detail?.title ?: video.title,
                    author = detail?.ownerName ?: video.ownerName,
                    authorFace = detail?.ownerFace ?: video.ownerFace,
                    onlineText = onlineText,
                    onlineCount = onlineCount,
                    followed = followed,
                    liked = liked,
                    likeCount = likedCount,
                    replyCount = detail?.reply ?: 0L,
                    favorited = favorited,
                    favoriteCount = favoriteCount,
                    shareCount = shareCount,
                    qualityLabel = if (autoQuality) "自动" else activeQuality?.label ?: "",
                    cover = detail?.cover ?: video.cover,
                    pages = detail?.playerPages().orEmpty(),
                    currentCid = currentCid,
                    folders = folders,
                    folderIds = folderIds,
                    loggedIn = api.isLoggedIn,
                    subtitles = subtitles,
                    selectedSubtitle = selectedSubtitle,
                    subtitleCues = subtitleCues,
                    danmakuItems = danmaku,
                ),
                onLikeClick = { toggleLike() },
                onDislikeClick = { toggleDislike() },
                onCommentClick = {
                    val d = detail
                    if (d != null) {
                        fullscreen = false
                        onOpenComments(d)
                    }
                },
                onShareClick = { shareVideo() },
                onOpenSpaceClick = { toggleFollow() },
                onToggleFolder = { toggleFolder(it) },
                onRequestFavorites = { ensureFavoritesLoaded() },
                onPageSelected = { switchPage(it) },
                onSubtitleSelected = { selectedSubtitle = it },
                onAddWatchLater = { addWatchLater() },
                onPlayStateChanged = { playing = it },
                onToggleFullscreen = { fullscreen = !fullscreen },
                onBackClick = {
                    when {
                        fullscreen -> fullscreen = false
                        else -> leaveVideo()
                    }
                },
                onHomeClick = { leaveVideo() },
                resumePositionMs = resumePositionMs,
                onQualitySelected = { switchQuality(it) },
                onCacheVideo = { cacheVideo(it) },
                immersiveStatusBar = immersiveStatusBar,
                pictureInPicture = pictureInPicture,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 随着收起进度给整个播放器盖一层粉色遮罩；收满时完全不透明。
        // 遮罩高度就是播放器块高度，所以状态栏那一条也会一起变粉。
        if (!fullscreen && collapseProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(playerBlockHeight)
                    .background(BrandPink.copy(alpha = collapseProgress)),
            )
        }

        // 粉色遮罩快盖满时，在顶部 56dp 里显示播放入口（接近到顶才出现）。
        val collapsedPlayAlpha = ((collapseProgress - 0.85f) / 0.15f).coerceIn(0f, 1f)
        if (!fullscreen && collapsedPlayAlpha > 0.01f) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusTop)
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer { alpha = collapsedPlayAlpha }
                    .clickable(interactionSource = null, indication = null) {
                        // 点“立即播放”：先让播放器展开，再开始播放。
                        collapseState.restore()
                        if (!playing) BackgroundPlayback.togglePlayPause()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // 和播放器控件同一个播放图标（TeakIcon.PlayerPlay），无底色、纯白。
                    PlayGlyph(tint = Color.White, size = 16.dp)
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "立即播放",
                    color = Color.White,
                    fontSize = 15.sp,
                )
            }
        }
    }

    TeakActionSheet(
        visible = coinSheet,
        onDismiss = { coinSheet = false },
        title = "请选择投币数量",
    ) {
        SheetActionRow("投 1 枚硬币", MenuGlyphKind.Coin, MiuixTheme.colorScheme.onBackground) {
            doCoin(1)
        }
        SheetActionRow("投 2 枚硬币", MenuGlyphKind.Coin, MiuixTheme.colorScheme.onBackground) {
            doCoin(2)
        }
    }

    com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheet(
        show = favSheet,
        state = com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheetUiState(
            title = "收藏到", description = "可选择多个收藏夹，点选后立即保存",
            folders = folders.map { folder ->
                com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderOption(
                    folder.id, folder.title, folder.count, folder.id in folderIds,
                )
            },
            loading = favLoading, pendingFolderId = pendingFavoriteFolder,
        ),
        onDismiss = { favSheet = false },
        onSelect = ::toggleFolder,
    )
}

@Composable
private fun ActionStat(
    modifier: Modifier = Modifier,
    label: String,
    value: Long,
    kind: ActionKind,
    active: Boolean = false,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    var isPressing by remember { mutableStateOf(false) }
    var press by remember { mutableFloatStateOf(0f) }
    // 单击投币时模糊回弹时间固定 230ms，其它操作 160ms
    val pressDecayMs = if (kind == ActionKind.Coin) 230f else 160f
    // 按住期间一直保持压满状态，松开后才回弹
    LaunchedEffect(isPressing) {
        if (isPressing) {
            press = 1f
        } else {
            // 首帧 press 本来就是 0，别把它“回弹”成 1 再落回去
            if (press <= 0.01f) return@LaunchedEffect
            val startNanos = System.nanoTime()
            while (true) {
                val t = ((System.nanoTime() - startNanos) / 1_000_000f / pressDecayMs)
                    .coerceIn(0f, 1f)
                press = 1f - t
                if (t >= 1f) break
                kotlinx.coroutines.delay(8)
            }
            press = 0f
        }
    }
    // 变粉 / 恢复都走渐变动画；按住期间保持粉色，松手后等按压回弹结束再渐隐
    val colorTarget = if (active || isPressing || press > 0.01f) BrandPink else colors.onBackgroundVariant
    val animatedTint by androidx.compose.animation.animateColorAsState(
        targetValue = colorTarget,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 240,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "action-tint",
    )

    var longPressFired by remember { mutableStateOf(false) }
    var rippleHolding by remember { mutableStateOf(false) }
    var rippleBurst by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    fun defaultVibrator(): android.os.Vibrator? {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                as? android.os.VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
        return vibrator?.takeIf { it.hasVibrator() }
    }
    /** 按住期间脉冲式的轻震动，和小水波同一个节奏（170ms 一下）。 */
    fun pulseVibrate() {
        val vibrator = defaultVibrator() ?: return
        runCatching {
            vibrator.vibrate(
                android.os.VibrationEffect.createOneShot(
                    26L,
                    72,
                ),
            )
        }
    }
    fun strongVibrate() {
        val vibrator = defaultVibrator() ?: return
        vibrator.cancel()
        // 一下大的长震，而不是连续两下
        vibrator.vibrate(
            android.os.VibrationEffect.createOneShot(
                420,
                255,
            ),
        )
    }
    LaunchedEffect(isPressing, onLongPress) {
        if (!isPressing) {
            rippleHolding = false
            return@LaunchedEffect
        }
        longPressFired = false
        if (onLongPress == null) return@LaunchedEffect
        delay(280)
        if (!isPressing) {
            rippleHolding = false
            return@LaunchedEffect
        }
        rippleHolding = true
        val start = System.nanoTime()
        var nextPulseNs = 0L
        while (isPressing && !longPressFired) {
            val now = System.nanoTime()
            val elapsed = (now - start) / 1_000_000f
            // 大震点必须落在小震的同一节拍上：小震在 0/320/640/960ms，
            // 1280ms 先判完成、不再补小震，于是最后一下小震到大震正好也是 320ms。
            if (elapsed >= 1280f) {
                longPressFired = true
                rippleHolding = false
                rippleBurst += 1
                strongVibrate()
                onLongPress()
            } else if (now >= nextPulseNs) {
                nextPulseNs = now + 320_000_000L
                pulseVibrate()
            }
            delay(16)
        }
    }

    Column(
        modifier = modifier
            .width(58.dp)
            // 不要 clip：外扩的圆环需要画出 36dp 图标框之外
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    // 所有按钮按住期间都保持压满状态
                    isPressing = true
                    press = 1f
                    var canceled = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        // 父级翻页/滚动一旦接管，就不再算点击。
                        if (change.isConsumed ||
                            kotlin.math.abs(change.position.x - startX) > touchSlop ||
                            kotlin.math.abs(change.position.y - startY) > touchSlop
                        ) {
                            canceled = true
                            break
                        }
                    }
                    isPressing = false
                    if (!canceled && !longPressFired) onClick()
                }
            }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (onLongPress != null) {
                LikeRipple(
                    holding = rippleHolding,
                    burst = rippleBurst,
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        val s = 1f - 0.09f * press
                        scaleX = s
                        scaleY = s
                    }
                    .blur((2.8f * press).dp),
                contentAlignment = Alignment.Center,
            ) {
                ResourceGlyph(
                    res = kind.res,
                    tint = animatedTint,
                    size = 27.dp,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (value > 0) formatCount(value) else label,
            color = animatedTint,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class ActionKind(val res: Int) {
    ThumbUp(TeakIcon.Like),
    ThumbDown(TeakIcon.Dislike),
    Coin(TeakIcon.Coin),
    Star(TeakIcon.Star),
    Share(TeakIcon.Share),
}

@Composable
private fun MiniStat(text: String, playIcon: Boolean) {
    val colors = MiuixTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
            if (playIcon) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.3f, size.height * 0.12f)
                    lineTo(size.width * 0.3f, size.height * 0.88f)
                    lineTo(size.width * 0.9f, size.height * 0.5f)
                    close()
                }
                drawPath(path, colors.onBackgroundVariant)
            } else {
                drawRoundRect(
                    color = colors.onBackgroundVariant,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.15f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.15f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.1f),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(text = text, color = colors.onBackgroundVariant, fontSize = 12.sp)
    }
}

@Composable
private fun PersonGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 12.dp) {
    // 和播放器左上角「正在看」使用同一套双人观看图标。
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val stroke = 1.dp.toPx()
        val w = this.size.width
        val h = this.size.height
        drawCircle(
            color = color,
            radius = w * 0.15f,
            center = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.32f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawCircle(
            color = color,
            radius = w * 0.13f,
            center = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.3f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.5f),
            size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.38f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.48f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.36f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
    }
}

/**
 * 可折叠简介：用 TextMeasurer 量出完整高度，再按手动进度裁剪高度，
 * 不依赖 frame clock 的 animateContentSize（本项目里它不推进）。
 */
@Composable
private fun CollapsibleDescription(
    text: String,
    progress: Float,
    color: Color,
    backgroundColor: Color,
    onExpandableChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val linkColor = MiuixTheme.colorScheme.primary
    val biliTitles = rememberBiliLinkTitles(text)
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = androidx.compose.ui.text.TextStyle(
        color = color,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    )
    val annotatedText = androidx.compose.runtime.remember(text, linkColor, biliTitles) {
        linkifyText(
            text = text,
            linkColor = linkColor,
            titles = biliTitles,
            onOpenVideo = { bvid ->
                com.aritxonly.bdfun.TeakNavigator.openVideoByBvid(context, bvid)
            },
            onOpen = { uriHandler.openUri(it) },
        )
    }
    BoxWithConstraints(modifier) {
        val widthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        val measured = measurer.measure(
            text = annotatedText,
            style = style,
            maxLines = Int.MAX_VALUE,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
        )
        val fullPx = measured.size.height.toFloat()
        // 两行的真实高度与第二行结束位置：模糊只作用在这之后的部分
        val twoLineLayout = measurer.measure(
            text = annotatedText,
            style = style,
            maxLines = 2,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
        )
        val twoLinePx = twoLineLayout.size.height.toFloat()
        val collapsedPx = minOf(twoLinePx, fullPx)
        val tailText = if (twoLineLayout.lineCount >= 2) {
            val end = runCatching { twoLineLayout.getLineEnd(1, visibleEnd = false) }
                .getOrDefault(text.length)
                .coerceIn(0, text.length)
            text.substring(end)
        } else {
            ""
        }
        // 以「完整高度 > 两行高度」判定是否真的被折叠，比 hasVisualOverflow 稳
        LaunchedEffect(fullPx, collapsedPx) {
            if (fullPx > collapsedPx + 1f) onExpandableChange()
        }
        val p = progress.coerceIn(0f, 1f)
        // 给前两行下面再留 2dp 安全区：即使实际渲染行高和测量值有 1~2dp 偏差，
        // 模糊也进不到前两行
        val safeTopPx = collapsedPx + with(density) { 2.dp.toPx() }
        val targetPx = safeTopPx +
            (fullPx - safeTopPx).coerceAtLeast(0f) * p
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { targetPx.toDp() })
                .clipToBounds(),
        ) {
            // 先画第三行及以后（带模糊），再盖一层背景和清晰的前两行：
            // 即使模糊半径向上溢出，也会被背景层完全挡住
            if (tailText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .offset(y = with(density) { safeTopPx.toDp() })
                        .fillMaxWidth()
                        .height(with(density) { (fullPx - safeTopPx).coerceAtLeast(0f).toDp() }),
                ) {
                    Text(
                        text = linkifyText(
                            text = tailText,
                            linkColor = linkColor,
                            titles = biliTitles,
                            onOpenVideo = { bvid ->
                                com.aritxonly.bdfun.TeakNavigator.openVideoByBvid(context, bvid)
                            },
                            onOpen = { uriHandler.openUri(it) },
                        ),
                        style = style,
                        maxLines = Int.MAX_VALUE,
                        modifier = Modifier
                            .graphicsLayer { alpha = p }
                            .blur(((1f - p).coerceIn(0f, 1f) * 9f).dp),
                    )
                }
            }
            // 遮住前两行区域的模糊溢出
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { safeTopPx.toDp() })
                    .background(backgroundColor)
                    .zIndex(0.5f),
            )
            // 折叠态：两行 + 省略号（随进度淡出）
            Text(
                text = annotatedText,
                style = style,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { layout ->
                    if (layout.hasVisualOverflow) onExpandableChange()
                },
                modifier = Modifier
                    .graphicsLayer { alpha = 1f - p }
                    .zIndex(1f),
            )
            // 原有的前两行：展开后保持清晰，不参与模糊
            Text(
                text = annotatedText,
                style = style,
                maxLines = 2,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .graphicsLayer { alpha = p }
                    .zIndex(1f),
            )
        }
    }
}

/** 和简介一样的展开箭头：展开/收回都旋转，带补间动画。 */
@Composable
private fun ExpandChevron(expanded: Boolean) {
    val colors = MiuixTheme.colorScheme
    val progress = rememberManualProgress(if (expanded) 1f else 0f, 300f)
    Box(
        modifier = Modifier.size(width = 24.dp, height = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(width = 12.dp, height = 8.dp)
                .graphicsLayer { rotationZ = 180f * progress },
        ) {
            val w = size.width
            val h = size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.12f, h * 0.28f)
                lineTo(w * 0.5f, h * 0.72f)
                lineTo(w * 0.88f, h * 0.28f)
            }
            drawPath(
                path = path,
                color = colors.onBackgroundVariant,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.6.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
        }
    }
}

/** 多 P 分集：横向滑动查看、点一下切换。 */
@Composable
private fun VideoPageStrip(
    pages: List<com.aritxonly.bdfun.data.VideoPage>,
    currentCid: Long,
    onSelect: (com.aritxonly.bdfun.data.VideoPage) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val stripState = rememberLazyListState()
    val leftAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stripState.canScrollBackward) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "pageStripLeftMask",
    )
    val rightAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stripState.canScrollForward) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "pageStripRightMask",
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = stripState,
            modifier = Modifier.fillMaxWidth(),
            // 外层已经有 12dp；这里不再加，让第一个 P 的左边和合集圆角卡片左边缘对齐。
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = pages, key = { it.cid }) { page ->
                val active = page.cid == currentCid
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .clickable(interactionSource = null, indication = null) { onSelect(page) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = page.part.ifBlank { "P${page.page}" },
                        color = if (active) colors.primary else colors.onBackground,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // 左右各 6dp 渐变，提示这里还能横向滑动；到最左/最右才淡出。
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(6.dp)
                .height(54.dp)
                .graphicsLayer { alpha = leftAlpha }
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.background, colors.background.copy(alpha = 0f)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(6.dp)
                .height(54.dp)
                .graphicsLayer { alpha = rightAlpha }
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.background.copy(alpha = 0f), colors.background),
                    ),
                ),
        )
    }
}

/**
 * 播放器下方内容按从上到下依次浮出。
 * 用户反馈原来上浮拖沓：这里把错开间隔和单段时长都砍半
 * （60ms / 120ms → 30ms / 60ms），保留层次但整体更快。
 */
@Composable
private fun rememberStaggerProgress(ready: Boolean, order: Int): Float {
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(ready, order) {
        if (!ready) {
            target = 0f
        } else {
            kotlinx.coroutines.delay(order * 30L)
            target = 1f
        }
    }
    return rememberManualProgress(target, 60f)
}

@Composable
private fun StaggerLine(
    ready: Boolean,
    order: Int,
    content: @Composable () -> Unit,
) {
    val progress = rememberStaggerProgress(ready, order)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 10.dp.toPx()
            },
    ) {
        content()
    }
}

/** 合集：点开整张圆角卡片向下展开，内部可以上下滑查看更早/更晚的单集。 */
@Composable
private fun VideoCollectionCard(
    title: String,
    episodes: List<com.aritxonly.bdfun.data.VideoEpisode>,
    currentCid: Long,
    onSelect: (com.aritxonly.bdfun.data.VideoEpisode) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    var expanded by remember(title, episodes.size) { mutableStateOf(false) }
    // 点击合集标题栏按钮时，等展开动画让列表完成测量后再滚。
    // 0 = 顶部，1 = 当前集，2 = 底部。
    var pendingScrollTarget by remember { mutableStateOf<Int?>(null) }
    val currentIndex = episodes.indexOfFirst { it.cid == currentCid }.let {
        if (it < 0) 0 else it
    }
    val scrollState = rememberScrollState()
    // 记住每一集在合集内部的纵坐标，点「定位」时能准确滚到当前这一集。
    val episodeOffsets = remember { mutableStateMapOf<Long, Int>() }
    fun centeredScrollOffset(): Int {
        val cid = episodes.getOrNull(currentIndex)?.cid ?: return 0
        val raw = episodeOffsets[cid]
            ?: with(density) { (currentIndex * 62).dp.toPx() }.toInt()
        val halfViewport = with(density) { 99.dp.toPx() }
        val halfItem = with(density) { 31.dp.toPx() }
        return (raw - halfViewport + halfItem).toInt().coerceAtLeast(0)
    }
    // 展开后默认让当前播放的这一集停在合集区域的中间。
    LaunchedEffect(expanded) {
        if (!expanded) return@LaunchedEffect
        delay(40)
        val target = pendingScrollTarget
        pendingScrollTarget = null
        val value = when (target) {
            0 -> 0
            2 -> scrollState.maxValue
            else -> centeredScrollOffset()
        }
        scrollState.animateScrollTo(value)
    }
    fun scrollCollection(target: Int) {
        scope.launch {
            if (!expanded) {
                pendingScrollTarget = target
                expanded = true
            } else {
                delay(40)
                val value = when (target) {
                    0 -> 0
                    2 -> scrollState.maxValue
                    else -> centeredScrollOffset()
                }
                scrollState.animateScrollTo(value)
            }
        }
    }
    // Miuix 负责回弹，这一层负责把回弹之后剩下的位移/惯性吃掉，
    // 否则快速一甩仍会把速度传给整页。
    val keepInsideCollection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = Offset(0f, available.y)

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = Velocity(0f, available.y)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = null, indication = null) { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Playlist,
                contentDescription = null,
                tint = colors.onBackground,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "合集 · $title",
                color = colors.onBackground,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            // 一键回到顶部，紧贴在合集名称右边。
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        // 只回到合集内部的顶部，不把整个视频页滚回最上面。
                        scrollCollection(0)
                    },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    val stroke = 1.6.dp.toPx()
                    val cx = size.width / 2f
                    val topY = size.height * 0.16f
                    val bottomY = size.height * 0.86f
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, bottomY),
                        end = androidx.compose.ui.geometry.Offset(cx, topY),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, topY),
                        end = androidx.compose.ui.geometry.Offset(
                            size.width * 0.20f,
                            size.height * 0.44f,
                        ),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, topY),
                        end = androidx.compose.ui.geometry.Offset(
                            size.width * 0.80f,
                            size.height * 0.44f,
                        ),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            }
            // 一键到底：排在「一键到顶」右边。
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        scrollCollection(2)
                    },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    val stroke = 1.6.dp.toPx()
                    val cx = size.width / 2f
                    val topY = size.height * 0.14f
                    val bottomY = size.height * 0.84f
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, topY),
                        end = androidx.compose.ui.geometry.Offset(cx, bottomY),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, bottomY),
                        end = androidx.compose.ui.geometry.Offset(
                            size.width * 0.20f,
                            size.height * 0.56f,
                        ),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, bottomY),
                        end = androidx.compose.ui.geometry.Offset(
                            size.width * 0.80f,
                            size.height * 0.56f,
                        ),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            }
            // 定位到当前播放的这一集：展开合集并滚到当前集。
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        scrollCollection(1)
                    },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    val stroke = 1.5.dp.toPx()
                    val c = center
                    drawCircle(
                        color = colors.onBackgroundVariant,
                        radius = size.minDimension * 0.30f,
                        center = c,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                    )
                    drawCircle(
                        color = colors.onBackgroundVariant,
                        radius = size.minDimension * 0.10f,
                        center = c,
                    )
                    val arm = size.minDimension * 0.5f
                    val gap = size.minDimension * 0.38f
                    listOf(
                        androidx.compose.ui.geometry.Offset(c.x, c.y - arm) to
                            androidx.compose.ui.geometry.Offset(c.x, c.y - gap),
                        androidx.compose.ui.geometry.Offset(c.x, c.y + gap) to
                            androidx.compose.ui.geometry.Offset(c.x, c.y + arm),
                        androidx.compose.ui.geometry.Offset(c.x - arm, c.y) to
                            androidx.compose.ui.geometry.Offset(c.x - gap, c.y),
                        androidx.compose.ui.geometry.Offset(c.x + gap, c.y) to
                            androidx.compose.ui.geometry.Offset(c.x + arm, c.y),
                    ).forEach { (start, end) ->
                        drawLine(
                            color = colors.onBackgroundVariant,
                            start = start,
                            end = end,
                            strokeWidth = stroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${currentIndex + 1}/${episodes.size}",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.width(52.dp),
            )
            Spacer(Modifier.width(2.dp))
            ExpandChevron(expanded = expanded)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(198.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(keepInsideCollection)
                        .overScrollVertical(nestedScrollToParent = false)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 8.dp),
                ) {
                    episodes.forEachIndexed { index, episode ->
                        val active = episode.cid == currentCid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned {
                                    episodeOffsets[episode.cid] = it.positionInParent().y.toInt()
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (active) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                                )
                                .clickable(interactionSource = null, indication = null) {
                                    onSelect(episode)
                                }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(84.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surface),
                            ) {
                                AsyncImage(
                                    model = rememberImageRequest(episode.cover),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (episode.duration > 0) {
                                    Text(
                                        text = formatDuration(episode.duration),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        lineHeight = 11.sp,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 5.dp, bottom = 4.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = episode.title,
                                    color = if (active) colors.primary else colors.onBackground,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val meta = buildList {
                                    if (episode.pubdate > 0) add(formatDateTime(episode.pubdate))
                                    if (episode.play > 0) add("${formatCount(episode.play)} 播放")
                                    if (episode.danmaku > 0) add("${formatCount(episode.danmaku)} 弹幕")
                                }
                                if (meta.isNotEmpty()) {
                                    Text(
                                        text = meta.joinToString(" · "),
                                        color = colors.onBackgroundVariant,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                // 上下 4dp 渐变，让列表在卡片边缘自然消失。
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(colors.surface, colors.surface.copy(alpha = 0f)),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(colors.surface.copy(alpha = 0f), colors.surface),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
internal fun RelatedRow(
    item: VideoItem,
    onClick: () -> Unit,
    metaLabel: String? = null,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val oneTake = rememberOneTakeAnchor()
    val rowRadiusPx = with(LocalDensity.current) { 12.dp.toPx() }
    val coverWidth = 150.dp
    val coverHeight = coverWidth * 9f / 16f
    Row(
        modifier = oneTake.modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) {
                oneTake.open(context, item, rowRadiusPx) { onClick() }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(coverWidth)
                .height(coverHeight)
                .clip(ContinuousCornerShape(10.dp))
                .background(colors.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = rememberImageRequest(item.cover, crossfade = false),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // 时长直接压在封面上，不加任何背景
            if (item.duration > 0) {
                Text(
                    text = formatDuration(item.duration),
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 5.dp, bottom = 4.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        // 右列高度锁成封面高度：标题第一行贴封面顶部，UP 行贴封面底部
        Column(
            modifier = Modifier.weight(1f).height(coverHeight),
        ) {
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.padding(start = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetricGlyph(play = true, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(item.play),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.width(6.dp))
                MetricGlyph(play = false, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(item.danmaku),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            if (metaLabel != null) {
                Text(
                    text = metaLabel,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UpBadge()
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = item.ownerName,
                        color = colors.onBackgroundVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}


/**
 * 「自动」清晰度：按系统给出的当前网络下行带宽选一档。
 * 不看 WiFi/移动网络类型，只按实际带宽走；带宽未知时保守选 720P。
 */
private fun autoQualityFor(
    qualities: List<Quality>,
    context: android.content.Context,
): Quality? {
    if (qualities.isEmpty()) return null
    val mbps = runCatching {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        (caps?.linkDownstreamBandwidthKbps ?: 0) / 1024.0
    }.getOrDefault(0.0)
    val target = when {
        mbps >= 40.0 -> 127
        mbps >= 24.0 -> 120
        mbps >= 10.0 -> 112
        mbps >= 6.0 -> 80
        mbps >= 3.0 -> 64
        mbps >= 1.5 -> 32
        mbps > 0.0 -> 16
        else -> 64
    }
    return autoQualityLadder
        .firstOrNull { id -> id <= target && qualities.any { it.id == id } }
        ?.let { id -> qualities.first { it.id == id } }
        ?: qualities.minByOrNull { it.id }
}

/** 自动选档只走常规 SDR 阶梯，避开杜比视界/HDR，避免自动切到设备解不了的流。 */
private val autoQualityLadder = listOf(127, 120, 116, 112, 80, 74, 64, 32, 16)

private data class VideoCacheEntry(
    val detail: VideoDetail,
    val playInfo: PlayInfo?,
    val danmaku: List<DanmakuItem>,
    val related: List<VideoItem>,
)

/** 真正退出视频页时调用；从相关推荐进另一个视频不会走这里。 */
internal fun clearRelatedMemory(bvid: String) {
    if (bvid.isNotBlank()) RelatedMemory.remove(bvid)
}

/** 相关推荐单独存一份：页面被重新组合时先把列表放回去，不会先空一下再出现。 */
private object RelatedMemory {
    private val map = HashMap<String, List<VideoItem>>()
    operator fun get(bvid: String): List<VideoItem>? = map[bvid]
    operator fun set(bvid: String, value: List<VideoItem>) {
        if (value.isNotEmpty()) map[bvid] = value
        while (map.size > 12) {
            val first = map.keys.firstOrNull() ?: break
            map.remove(first)
        }
    }

    fun remove(bvid: String) {
        map.remove(bvid)
    }
}

/** 视频详情的内存缓存：从相关推荐返回时不再重新请求。 */
private object VideoCache {    private val map = LinkedHashMap<String, VideoCacheEntry>()

    fun get(bvid: String): VideoCacheEntry? = map[bvid]

    fun put(bvid: String, entry: VideoCacheEntry) {
        map[bvid] = entry
        while (map.size > 6) {
            val first = map.keys.firstOrNull() ?: break
            map.remove(first)
        }
    }

    /** 点赞 / 投币 / 收藏后同步缓存，退出再进来仍然显示已操作状态。 */
    fun update(bvid: String, transform: (VideoDetail) -> VideoDetail) {
        val entry = map[bvid] ?: return
        map[bvid] = entry.copy(detail = transform(entry.detail))
    }
}
