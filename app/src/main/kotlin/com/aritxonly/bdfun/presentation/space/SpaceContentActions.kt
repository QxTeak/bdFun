package com.aritxonly.bdfun

import com.aritxonly.bdfun.core.model.user.SpaceSeason
import com.aritxonly.bdfun.ui.showToast
import kotlinx.coroutines.launch

internal class SpaceContentActions(runtime: SpaceScreenRuntime, private val context: android.content.Context) {
    private val content = runtime.content
    private val controller = runtime.controller
    private val scope = runtime.scope
    private val isSelf = content.isSelf
    private val loggedIn = content.loggedIn
    private val ui = content.ui
    private val pagerState = content.pagerState
    private val followBusy get() = controller.state.value.profile.followBusy
    fun loadVideos(page: Int) { scope.launch { controller.loadVideos(page) } }
    fun loadArticles(page: Int) { scope.launch { controller.loadArticles(page) } }
    fun loadShop() { scope.launch { controller.loadShop() } }
    fun loadMoreFeeds() { scope.launch { controller.loadMoreFeeds() } }
    fun loadBangumi(page: Int) { scope.launch { controller.loadBangumi(page) } }
    fun loadMore(tab: Int, submissionMode: Int) {
        when (val request = controller.nextPage(tab, submissionMode)) {
            is com.aritxonly.bdfun.feature.space.application.SpacePageRequest.Videos -> loadVideos(request.page)
            is com.aritxonly.bdfun.feature.space.application.SpacePageRequest.Articles -> loadArticles(request.page)
            is com.aritxonly.bdfun.feature.space.application.SpacePageRequest.Bangumi -> loadBangumi(request.page)
            com.aritxonly.bdfun.feature.space.application.SpacePageRequest.Feeds -> loadMoreFeeds()
            null -> Unit
        }
    }
    fun loadCollection(collection: SpaceSeason) {
        ui.selectedCollection = collection
        ui.submissionMode = 2
        ui.selectedTab = 2
        scope.launch { pagerState.animateScrollToPage(2) }
        scope.launch { controller.loadCollection(collection) }
    }

    fun follow() {
        if (isSelf) context.showToast("编辑资料暂未接入")
        else if (!loggedIn) context.showToast("请先登录")
        else if (!followBusy) scope.launch { controller.toggleFollow()?.let { context.showToast(it) } }
    }
}
