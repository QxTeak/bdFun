package com.aritxonly.bdfun

import android.content.Context
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.SentHistoryStore
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.data.UserProfile
import com.aritxonly.bdfun.data.VideoItem

/**
 * 进程级单例：多 Activity 共用同一套 api / settings / 登录态 / 待打开的视频。
 */
object AppGraph {
    private var container: com.aritxonly.bdfun.di.AppContainer? = null

    internal val themePalette get() = container?.themePalette

    internal val liveReads get() = checkNotNull(container) { "AppGraph 尚未初始化" }.liveReads
    internal fun liveDanmakuConnection(scope: kotlinx.coroutines.CoroutineScope) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.liveDanmakuConnection(scope)
    internal fun createLivePlayback(context: android.content.Context) = checkNotNull(container).createLivePlayback(context)
    internal fun liveRoomState(id: Long, scope: kotlinx.coroutines.CoroutineScope,
        connection: com.aritxonly.bdfun.core.domain.LiveDanmakuConnection, playback: com.aritxonly.bdfun.core.domain.LivePlayback) =
        checkNotNull(container).liveRoomState(id, scope, connection, playback)
    val api: BiliApi get() = checkNotNull(container) { "AppGraph 尚未初始化" }.api
    val settings: SettingsStore get() = checkNotNull(container) { "AppGraph 尚未初始化" }.settings
    val sentHistory: com.aritxonly.bdfun.core.domain.SentHistoryRepository
        get() = checkNotNull(container?.sentHistory) { "AppGraph 尚未使用 Context 初始化" }

    val dynamicFeedState get() = checkNotNull(container) { "AppGraph 尚未初始化" }.dynamicFeedState
    internal fun onboardingStateHolder(scope: kotlinx.coroutines.CoroutineScope, step: Int) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.onboardingStateHolder(scope, step)
    internal fun inlineVideoStateHolder(bvid: String) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.inlineVideoStateHolder(bvid)
    internal fun inlinePlayback(context: Context, bvid: String, muted: Boolean) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.createInlinePlayback(context, bvid, muted)
    internal fun dynamicDetailStateHolder(initial: com.aritxonly.bdfun.data.FeedItem) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.dynamicDetailStateHolder(initial)
    internal fun privateChatStateHolder(session: com.aritxonly.bdfun.data.MessageSession) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.privateChatStateHolder(session)
    internal fun searchStateHolder() = checkNotNull(container) { "AppGraph 尚未初始化" }.searchStateHolder()
    internal fun messageSessionsStateHolder(pageType: Int = 1) = checkNotNull(container) { "AppGraph 尚未初始化" }.messageSessionsStateHolder(pageType)
    internal fun messageBadgeStateHolder(owner: Long?) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.messageBadgeStateHolder(owner)
    internal fun messageToolsStateHolder(tool: com.aritxonly.bdfun.feature.message.application.MessageTool) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.messageToolsStateHolder(tool)
    internal fun messageFeedStateHolder(kind: com.aritxonly.bdfun.feature.message.application.MessageFeedKind) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.messageFeedStateHolder(kind)
    internal fun settingsCdnStateHolder() = checkNotNull(container) { "AppGraph 尚未初始化" }.settingsCdnStateHolder()
    internal val mineLayout get() = checkNotNull(container) { "AppGraph 尚未初始化" }.mineLayout
    internal val mineDependencies get() = checkNotNull(container) { "AppGraph 尚未初始化" }.mineDependencies
    internal val libraryDependencies get() = checkNotNull(container) { "AppGraph 尚未初始化" }.libraryDependencies
    internal fun spacePageResources(mid: Long) = checkNotNull(container) { "AppGraph 尚未初始化" }.spacePageResources(mid)
    internal fun currentAutomaticCdn() = checkNotNull(container) { "AppGraph 尚未初始化" }.automaticCdn.currentService()
    internal suspend fun refreshAutomaticCdn() = checkNotNull(container) { "AppGraph 尚未初始化" }.automaticCdn.refreshSelection()
    internal fun relationFeedStateHolder(mid: Long, fans: Boolean, frequent: Boolean) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.relationFeedStateHolder(mid, fans, frequent)
    internal val videoCache get() = checkNotNull(container?.videoCache) { "AppGraph 尚未使用 Context 初始化" }
    internal val loginStatus get() = checkNotNull(container) { "AppGraph 尚未初始化" }.loginStatus
    internal fun incomingVideoLinks() = com.aritxonly.bdfun.core.application.IncomingVideoLinkStateHolder(
        checkNotNull(container) { "AppGraph 尚未初始化" }.videoLinkResolver)
    internal fun createQrLogin() = checkNotNull(container) { "AppGraph 尚未初始化" }.createQrLogin()
    internal val richTextDependencies get() = checkNotNull(container) { "AppGraph 尚未初始化" }.richTextDependencies
    internal val mediaControls get() = checkNotNull(container) { "AppGraph 尚未初始化" }.mediaControls
    internal val discDependencies get() = checkNotNull(container) { "AppGraph 尚未初始化" }.discDependencies
    internal val backgroundPlayback get() = checkNotNull(container) { "AppGraph 尚未初始化" }.backgroundPlayback
    internal fun videoPageState(scope: kotlinx.coroutines.CoroutineScope) =
        checkNotNull(container) { "AppGraph 尚未初始化" }.let {
            com.aritxonly.bdfun.ui.VideoPagePresentationState(scope, it.playbackPauses,
                it.videoReadCache.scope(), it.automaticVideoQuality, it.settings, it.videoPageDependencies)
        }
    val homeFeedState get() = checkNotNull(container) { "AppGraph 尚未初始化" }.homeFeedState

    /** 登录态：MainActivity 与 LoginActivity 共享，登录成功直接写这里。 */
    val session get() = checkNotNull(container) { "AppGraph 尚未初始化" }.session
    internal suspend fun refreshSession() = checkNotNull(container) { "AppGraph 尚未初始化" }.sessionController.refresh()
    internal fun logout() = checkNotNull(container) { "AppGraph 尚未初始化" }.sessionController.logout()

    @Synchronized
    fun init(api: BiliApi, settings: SettingsStore, context: Context? = null) {
        container = com.aritxonly.bdfun.di.AppContainer(api, settings, context)
    }

    @Synchronized
    fun ensure(context: Context) {
        if (container == null) container = com.aritxonly.bdfun.di.AppContainer.create(context)
    }
}
