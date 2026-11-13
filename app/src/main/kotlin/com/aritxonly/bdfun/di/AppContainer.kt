package com.aritxonly.bdfun.di

import com.aritxonly.bdfun.navigateToVideoByBvid
import android.content.Context
import com.aritxonly.bdfun.feature.feed.application.FeedStateHolder
import com.aritxonly.bdfun.feature.feed.presentation.FeedState
import com.aritxonly.bdfun.infra.bili.BiliFeedPageRepository
import com.aritxonly.bdfun.infra.bili.BiliRecommendationActionsRepository
import com.aritxonly.bdfun.feature.home.presentation.HomeFeedState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.application.SessionResourceSlot
import com.aritxonly.bdfun.core.application.identity
import com.aritxonly.bdfun.infra.bili.BiliAccountRepository
import com.aritxonly.bdfun.infra.bili.BiliCommentRepository
import com.aritxonly.bdfun.infra.bili.BiliFeedRepository
import com.aritxonly.bdfun.infra.bili.BiliVideoRepository
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.AppThemePalette
import com.aritxonly.bdfun.data.*
import com.aritxonly.teak.common.settings.TeakDebugLog

/** 使用 applicationContext，避免进程级依赖持有 Activity。 */
internal class AppContainer(
    val api: BiliApi,
    val settings: SettingsStore,
    context: Context?,
) {
    val themePalette = AppThemePalette { settings.colorStyle }
    private var closed = false
    private val presentationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val videoCache = context?.let { app ->
        com.aritxonly.bdfun.core.application.DownloadQueueStateHolder(
            com.aritxonly.bdfun.infra.storage.AndroidVideoCacheStorage(app),
            com.aritxonly.bdfun.infra.bili.BiliVideoCacheTransfer(),
            CoroutineScope(SupervisorJob() + Dispatchers.IO), Dispatchers.Main.immediate,
            { CacheService.start(app) }).also { it.initialize() }
    }
    val loginStatus: LoginStatus = LoginStatus { api.isLoggedIn }
    val videoLinkResolver: VideoLinkResolver = com.aritxonly.bdfun.infra.bili.BiliVideoLinkResolver()
    val liveReads by lazy {
        com.aritxonly.bdfun.feature.live.application.LiveReadAccess(
            com.aritxonly.bdfun.infra.bili.BiliLiveRepository(api), loginStatus,
            com.aritxonly.bdfun.infra.bili.mapper.BiliLiveMessageDecoder())
    }
    fun liveDanmakuConnection(scope: CoroutineScope): LiveDanmakuConnection =
        com.aritxonly.bdfun.infra.bili.live.BiliLiveConnection(
            com.aritxonly.bdfun.infra.bili.live.LiveDanmakuClient(scope, webSocketFactory = api.webSocketClient))
    fun createLivePlayback(context: Context): com.aritxonly.bdfun.feature.live.presentation.LivePlaybackRuntime =
        com.aritxonly.bdfun.playback.AndroidLivePlaybackRuntime(
            com.aritxonly.bdfun.infra.playback.Media3LivePlayback(context.applicationContext, com.aritxonly.bdfun.R.layout.teak_player_view))
    fun liveRoomState(id: Long, scope: CoroutineScope, connection: LiveDanmakuConnection, playback: LivePlayback) =
        com.aritxonly.bdfun.feature.live.application.LiveRoomStateHolder(id, liveFallbackRoom(),
            liveReads, connection, playback, scope,
            { if (loginStatus.isLoggedIn()) session.value?.mid ?: 0L else 0L }, log = { LogCollector.record("LIVE", it) })
    val accountRepository: AccountRepository = BiliAccountRepository(api::nav, api::qrGenerate, api::qrPoll, api::logout)
    fun relationFeedStateHolder(mid: Long, fans: Boolean, frequent: Boolean): com.aritxonly.bdfun.feature.relations.application.RelationFeedStateHolder {
        val identity = sessionController.identity
        return com.aritxonly.bdfun.feature.relations.application.RelationFeedStateHolder(
            com.aritxonly.bdfun.infra.bili.BiliRelationRepository(api), mid, fans, frequent,
            identity.mid, { sessionController.state.value.user?.mid }, { api.isLoggedIn },
            { !closed && sessionController.identity == identity })
    }
    fun createQrLogin() = com.aritxonly.bdfun.feature.login.application.QrLoginStateHolder(
        com.aritxonly.bdfun.infra.bili.BiliQrLoginRepository(api::qrGenerate, api::qrPoll))
    val sessionController = com.aritxonly.bdfun.core.application.SessionStateHolder(accountRepository)
    val session = com.aritxonly.bdfun.presentation.SessionPresentationStateHolder(sessionController, presentationScope)
    val videoReadCache = com.aritxonly.bdfun.feature.video.application.VideoReadCache {
        sessionController.state.value.let {
            com.aritxonly.bdfun.feature.video.application.VideoReadCacheOwner(it.user?.mid, it.revision)
        }
    }
    val automaticVideoQuality = AutomaticVideoQualitySelector(
        com.aritxonly.bdfun.infra.playback.AndroidNetworkBandwidthSource(context))
    val playbackProgress: PlaybackProgressRepository = com.aritxonly.bdfun.infra.storage.AndroidPlaybackProgressRepository(context)
    val playbackTrackingRepository = com.aritxonly.bdfun.infra.bili.BiliPlaybackTrackingRepository(api)
    val videoEntryRepository: VideoEntryRepository = com.aritxonly.bdfun.infra.bili.BiliVideoEntryRepository(api::view, api::viewByAid)
    val videoRepository: VideoRepository = BiliVideoRepository(api::view, api::related)
    val videoMetadataRepository: VideoMetadataRepository = com.aritxonly.bdfun.infra.bili.BiliVideoMetadataRepository(api)
    val videoActionsRepository: VideoActionsRepository = com.aritxonly.bdfun.infra.bili.BiliVideoActionsRepository(api)
    val videoPlaybackRepository: VideoPlaybackRepository = com.aritxonly.bdfun.infra.bili.BiliVideoPlaybackRepository(
        { request -> api.playInfo(request.detail, request.quality, request.cid, request.durationSec,
            request.codecPreference, request.cdnService) },
        api::danmaku,
        { detail -> api.cloudResumePositionMs(detail.aid, detail.cid, detail.duration) },
    )
    fun createPlaybackController(context: Context, decoderMode: Int): com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform =
        com.aritxonly.bdfun.playback.AndroidVideoPlaybackPlatform(
            com.aritxonly.bdfun.infra.playback.Media3PlaybackController(context.applicationContext, decoderMode), mediaControls)
    private val playbackSession = com.aritxonly.bdfun.core.application.PlaybackSessionStateHolder(presentationScope)
    val backgroundPlayback by lazy {
        com.aritxonly.bdfun.ui.BackgroundPlaybackStateHolder(playbackSession, presentationScope,
            com.aritxonly.bdfun.core.application.BackgroundQueueLoader(videoRepository, videoPlaybackRepository),
            com.aritxonly.bdfun.playback.backgroundPlaybackPorts { settings })
    }
    val discDependencies by lazy {
        com.aritxonly.bdfun.ui.DiscDependencies(backgroundPlayback, com.aritxonly.bdfun.ui.DiscDiagnostics(
            { if (BuildConfig.DEBUG) LiveCounters.discInstances++ },
            { if (BuildConfig.DEBUG) LiveCounters.discInstances-- },
            { message -> FreezeWatchdog.log("DISC", message) },
        ))
    }
    val playbackQueue = com.aritxonly.bdfun.core.application.PlaybackQueueStateHolder()
    val mediaControls = com.aritxonly.bdfun.core.application.MediaControlsRegistry()
    val playbackPauses = com.aritxonly.bdfun.core.application.PlaybackPauseRegistry()
    private val emoteImages by lazy { context?.let(::createEmoteImageLoader) }
    private val emoteStore = com.aritxonly.bdfun.core.application.EmoteStore(
        com.aritxonly.bdfun.infra.bili.BiliEmoteRepository(api),
        context?.let { com.aritxonly.bdfun.infra.storage.FileEmoteCache(java.io.File(it.filesDir, "emote_panels")) },
        { sessionController.state.value.user?.mid ?: 0L })
    val emotes = com.aritxonly.bdfun.presentation.EmotePresentationState(emoteStore, presentationScope)
    private val linkTitles = com.aritxonly.bdfun.core.application.VideoLinkTitleStore(
        com.aritxonly.bdfun.infra.bili.BiliVideoLinkTitleRepository({ api.view(it).title }, { api.viewByAid(it).bvid }),
    )
    val richTextDependencies by lazy {
        com.aritxonly.bdfun.ui.RichTextDependencies(emotes, linkTitles,
            { scope -> com.aritxonly.bdfun.core.application.EmojiPanelStateHolder(emoteStore, scope) },
            { context, bvid -> navigateToVideoByBvid(context, bvid) }, emoteImages)
    }
    val commentLikesStore = com.aritxonly.bdfun.core.application.CommentLikeStore()
    val commentLikes = com.aritxonly.bdfun.presentation.CommentLikePresentationState(commentLikesStore, presentationScope)
    val commentContentRepository: CommentContentRepository = com.aritxonly.bdfun.infra.bili.BiliCommentContentRepository(api)
    val commentRepository: CommentRepository = BiliCommentRepository(
        { oid, type, page -> api.commentsByOid(oid, type, page) },
        { oid, type, root, page -> api.repliesByOid(oid, type, root, page) },
    )
    val feedRepository: FeedRepository = BiliFeedRepository(
        { page -> api.recommend(page, settings.recommendWeb) },
        { offset -> api.dynamicPage("all", offset) },
    )
    private data class HomeSlot(val job: Job, val view: HomeFeedState)
    private val homeSlot = SessionResourceSlot({ sessionController.identity }, {
        val job = SupervisorJob(presentationScope.coroutineContext[Job])
        val scope = CoroutineScope(presentationScope.coroutineContext + job)
        val view = com.aritxonly.bdfun.feature.home.presentation.createHomeFeedState(
            feedRepository, videoRepository,
            BiliRecommendationActionsRepository(api::addWatchLater, api::dislikeRecommendation), scope)
        HomeSlot(job, view)
    }, { it.view.close(); it.job.cancel() })
    val homeFeedState: HomeFeedState get() = homeSlot.get().view
    // 首页「直播」「番剧」两段：与推荐页同一套壳，状态在各自 feature 下，接口走 infra 仓储。
    val livePageState by lazy {
        com.aritxonly.bdfun.feature.live.application.LivePageStateHolder(
            com.aritxonly.bdfun.infra.bili.BiliLiveDiscoverRepository(api))
    }
    val bangumiPageState by lazy {
        com.aritxonly.bdfun.feature.pgc.application.BangumiPageStateHolder(
            com.aritxonly.bdfun.infra.bili.BiliPgcDiscoverRepository(api))
    }
    private val dynamicRepository by lazy {
        BiliFeedPageRepository({ type, offset, mid -> api.dynamicPage(type, offset, mid) }, api::dynamicPortal, api::dynamicUpList, api::likeFeed)
    }
    private data class FeedSlot(val job: Job, val view: FeedState)
    private val dynamicSlot = SessionResourceSlot({ sessionController.identity }, {
        val job = SupervisorJob(presentationScope.coroutineContext[Job])
        val scope = CoroutineScope(presentationScope.coroutineContext + job)
        FeedSlot(job, FeedState(FeedStateHolder(dynamicRepository), scope))
    }, { it.view.close(); it.job.cancel() })
    val dynamicFeedState: FeedState get() = dynamicSlot.get().view
    private val commentImageRepository by lazy { com.aritxonly.bdfun.infra.bili.BiliCommentImageRepository(api) }
    private val inlineVideoRepository by lazy {
        com.aritxonly.bdfun.infra.bili.BiliInlineVideoRepository(api::view) { detail, preferences ->
            api.playInfo(detail, preferences.quality, codecPreference = preferences.codecPreference,
                cdnService = preferences.cdnService)
        }
    }
    fun inlineVideoStateHolder(bvid: String) =
        com.aritxonly.bdfun.feature.feed.application.InlineVideoStateHolder(inlineVideoRepository, bvid) {
            InlineVideoPreferences(settings.dynamicAutoPlayQuality, settings.codecPreference, settings.cdnService)
        }
    fun createInlinePlayback(context: Context, bvid: String, muted: Boolean): com.aritxonly.bdfun.ui.InlinePlaybackRuntime {
        val controller = com.aritxonly.bdfun.infra.playback.Media3InlinePlaybackController(context, muted, BiliApi.UA,
            mapOf("Referer" to "https://www.bilibili.com/video/$bvid/"))
        return com.aritxonly.bdfun.ui.InlinePlaybackRuntime(controller, controller::bindSurface)
    }
    private val dynamicDetailRepository by lazy {
        com.aritxonly.bdfun.infra.bili.BiliFeedDetailRepository(api::dynamicDetail, api::userIpLocation, api::likeFeed)
    }
    fun dynamicDetailStateHolder(initial: com.aritxonly.bdfun.data.FeedItem) =
        com.aritxonly.bdfun.feature.feed.application.FeedDetailStateHolder(dynamicDetailRepository, initial)
    private val privateChatRepository = com.aritxonly.bdfun.infra.bili.BiliPrivateChatRepository(api)
    fun privateChatStateHolder(session: MessageSession): com.aritxonly.bdfun.feature.message.application.PrivateChatStateHolder {
        val identity = sessionController.identity
        return com.aritxonly.bdfun.feature.message.application.PrivateChatStateHolder(privateChatRepository, session,
            isCurrentOwner = { !closed && sessionController.identity == identity })
    }
    private val searchRepository = com.aritxonly.bdfun.infra.bili.BiliSearchRepository(api)
    fun searchStateHolder() = com.aritxonly.bdfun.feature.search.application.SearchStateHolder(searchRepository)
    fun messageSessionsStateHolder(pageType: Int = 1): com.aritxonly.bdfun.feature.message.application.MessageSessionsStateHolder {
        val identity = sessionController.identity
        return com.aritxonly.bdfun.feature.message.application.MessageSessionsStateHolder(
            com.aritxonly.bdfun.infra.bili.BiliMessageSessionsRepository(api),
            { !closed && sessionController.identity == identity }, pageType)
    }
    fun messageToolsStateHolder(tool: com.aritxonly.bdfun.feature.message.application.MessageTool):
        com.aritxonly.bdfun.feature.message.application.MessageToolsStateHolder {
        val identity = sessionController.identity
        return com.aritxonly.bdfun.feature.message.application.MessageToolsStateHolder(
            com.aritxonly.bdfun.infra.bili.BiliMessageManagementRepository(api), tool,
            { !closed && sessionController.identity == identity })
    }
    private val messageBadgeRepository = com.aritxonly.bdfun.infra.bili.BiliMessageBadgeRepository(api::messageUnreadCount)
    fun messageBadgeStateHolder(owner: Long?) = com.aritxonly.bdfun.feature.message.application.MessageBadgeStateHolder(
        messageBadgeRepository, owner,
    )
    fun messageFeedStateHolder(kind: com.aritxonly.bdfun.feature.message.application.MessageFeedKind):
        com.aritxonly.bdfun.feature.message.application.MessageFeedStateHolder {
        val identity = sessionController.identity
        return com.aritxonly.bdfun.feature.message.application.MessageFeedStateHolder(
            com.aritxonly.bdfun.infra.bili.BiliMessageFeedRepository(api), kind,
            { !closed && sessionController.identity == identity })
    }
    val automaticCdn = com.aritxonly.bdfun.infra.bili.AutoCdnSelector(api::cdnSampleUrl,
        context?.let { com.aritxonly.bdfun.infra.storage.AndroidAutoCdnCache(it) }, LogCollector::record)
    fun settingsCdnStateHolder() = com.aritxonly.bdfun.feature.settings.application.CdnSettingsStateHolder(automaticCdn)
    val mineLayout = com.aritxonly.bdfun.core.application.MineLayoutStateHolder(
        context?.let { com.aritxonly.bdfun.infra.storage.AndroidMineLayoutPreferences(it) }
            ?: object : MineLayoutPreferences {
                private var layout = com.aritxonly.bdfun.core.model.settings.MineLayout()
                override fun read() = layout
                override fun write(layout: com.aritxonly.bdfun.core.model.settings.MineLayout) { this.layout = layout }
            })
    private val mineProfileStore = com.aritxonly.bdfun.feature.mine.application.MineProfileStore(System::currentTimeMillis)
    private val mineRepository = com.aritxonly.bdfun.infra.bili.BiliMineRepository(api)
    val mineDependencies by lazy {
        com.aritxonly.bdfun.ui.MineDependencies({ profile ->
            val identity = sessionController.identity
            com.aritxonly.bdfun.feature.mine.application.MineStateHolder(profile, mineRepository, mineProfileStore,
                { sessionController.identity == identity && identity.mid == profile?.mid })
        }, { sentHistory?.all()?.take(3).orEmpty() }, mineLayout, { profile ->
            val identity = sessionController.identity
            com.aritxonly.bdfun.feature.mine.application.MinePreviewStateHolder(
                com.aritxonly.bdfun.infra.bili.BiliLibraryRepository(api),
                { !closed && sessionController.identity == identity && identity.mid == profile?.mid })
        })
    }
    val libraryDependencies by lazy {
        com.aritxonly.bdfun.ui.LibraryDependencies(::libraryStateHolder, { scope ->
            com.aritxonly.bdfun.feature.library.application.LibraryInteractionStateHolder(
                settings.repository.general, settings.state.map { Unit }, scope,
            )
        }, com.aritxonly.bdfun.feature.library.application.LibraryEntranceRegistry(), playbackQueue)
    }
    fun libraryStateHolder(): com.aritxonly.bdfun.feature.library.application.LibraryStateHolder {
        val identity = sessionController.identity
        return com.aritxonly.bdfun.feature.library.application.LibraryStateHolder(
            com.aritxonly.bdfun.infra.bili.BiliLibraryRepository(api), { sessionController.identity == identity },
        )
    }
    fun spacePageResources(mid: Long) = SessionResourceSlot({ sessionController.identity }, {
        val identity = sessionController.identity
        com.aritxonly.bdfun.feature.space.application.SpaceStateHolder(
            mid, com.aritxonly.bdfun.infra.bili.BiliSpaceRepository(api),
            { !closed && sessionController.identity == identity })
    }, { it.close() })
    fun onboardingStateHolder(scope: CoroutineScope, step: Int) =
        com.aritxonly.bdfun.feature.onboarding.application.OnboardingStateHolder(
            settings.repository.appearance, settings.repository.general, settings.state.map { Unit }, scope, step,
        )
    val videoStateFactory by lazy {
        com.aritxonly.bdfun.feature.video.application.VideoStateFactory(
            videoEntryRepository, videoRepository, videoPlaybackRepository, videoMetadataRepository,
            videoActionsRepository, playbackTrackingRepository, playbackTrackingRepository)
    }
    val videoPageDependencies by lazy {
        com.aritxonly.bdfun.ui.VideoPageDependencies(videoStateFactory, playbackProgress,
            { checkNotNull(videoCache) { "视频下载存储尚未初始化" } },
            { checkNotNull(sentHistory) { "发送记录存储尚未初始化" } },
            backgroundPlayback, playbackQueue, ::createPlaybackController, LogCollector::record,
            com.aritxonly.bdfun.ui.CommentDependencies(
                com.aritxonly.bdfun.feature.comment.application.CommentStateFactory(
                    commentContentRepository, commentImageRepository, commentLikesStore),
                session, commentLikes, commentLikesStore.generationState))
    }
    fun close() { closed = true; homeSlot.close(); dynamicSlot.close(); videoReadCache.close(); settings.close(); presentationScope.cancel() }
    val sentHistory: SentHistoryStore? = context?.let { SentHistoryStore(it.applicationContext) }

    init {
        presentationScope.launch {
            var previousIdentity = sessionController.identity
            sessionController.state.collect { state ->
                if (state.identity != previousIdentity) {
                    mineProfileStore.clear()
                    commentLikesStore.clear()
                    emoteStore.clear()
                    linkTitles.clear()
                    playbackQueue.clear()
                    dynamicSlot.clearStale()
                    homeSlot.clearStale()
                    previousIdentity = state.identity
                }
            }
        }
        TeakDebugLog.enabled = BuildConfig.DEBUG
        TeakDebugLog.sink = if (BuildConfig.DEBUG) { tag, message ->
            LogCollector.record(tag, message)
            if (tag == "MENU") LogCollector.flushSync()
        } else null
        context?.applicationContext?.let { app ->
            if (BuildConfig.DEBUG) FreezeWatchdog.start(app)
        }
    }

    companion object {
        fun create(context: Context): AppContainer {
            val app = context.applicationContext
            return AppContainer(BiliApi(app), SettingsStore(app), app)
        }
    }
}
