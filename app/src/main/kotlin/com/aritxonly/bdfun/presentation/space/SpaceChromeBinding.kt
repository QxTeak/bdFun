package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun SpaceChromeBinding(
    content: SpaceScreenContent, scroll: SpaceScrollContext, progress: SpaceScrollProgress,
    chrome: SpaceChromeState, actions: SpaceScrollActions,
) {
    val banner = content.banner
    val pagerState = content.pagerState
    val pagePosition = content.pagePosition
    val profile = content.snapshot.profile.space?.profile
    val loading = content.snapshot.profile.loading || !content.banner.ready
    val wideSpace = scroll.layout.wideSpace
    val profileWidth = scroll.layout.profileWidth
    val pageChrome = scroll.pageChrome
    val chromeVisible = !loading && banner.ready
    val chromeSelected = pagerState.currentPage

    SideEffect {
        chrome.wide = wideSpace
        chrome.title = profile?.name.orEmpty().ifBlank { "个人主页" }
        chrome.profileWidth = profileWidth
        chrome.visible = chromeVisible
        chrome.selected = chromeSelected
        chrome.position = pagePosition
        chrome.onSelect = actions::selectTab
    }
    // 滚动位置每帧都在变：只让它们驱动顶栏，不再让整个空间页跟着重组。
    LaunchedEffect(progress, pageChrome, chrome) {
        snapshotFlow {
            Triple(progress.blur.value, progress.anchor.value, pageChrome.materialProgress)
        }.distinctUntilChanged().collect { (blur, anchor, material) ->
            chrome.headerProgress = blur
            chrome.tabY = anchor
            chrome.contentProgress = material
        }
    }

}
