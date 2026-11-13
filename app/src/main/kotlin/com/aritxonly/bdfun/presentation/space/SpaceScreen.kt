package com.aritxonly.bdfun

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.ui.LocalPhotoViewer
import com.aritxonly.bdfun.ui.PhotoViewerHost
import com.aritxonly.bdfun.ui.PhotoViewerStateHolder

@Composable
internal fun SpaceScreen(
    mid: Long, session: com.aritxonly.bdfun.core.model.user.UserProfile?,
    controller: com.aritxonly.bdfun.feature.space.application.SpaceStateHolder, onTitleChange: (String) -> Unit, onBarBlurFractionChange: (Float) -> Unit,
    onBannerDarkChange: (Boolean) -> Unit, chrome: SpaceChromeState,
) {
    val context = LocalContext.current
    val runtime = rememberSpaceScreenRuntime(mid, session, controller)
    val content = runtime.content
    val contentActions = SpaceContentActions(runtime, context)
    SpaceContentEffects(runtime, contentActions, onTitleChange, onBannerDarkChange)
    val scroll = rememberSpaceScrollContext(content)
    val scrollActions = SpaceScrollActions(content, scroll, runtime.scope)
    val progress = SpaceScrollProgress(rememberSpaceBarBlur(scroll), rememberSpaceTabAnchor(scroll))
    SpaceScrollMaterialEffects(scroll, progress, onBarBlurFractionChange)
    SpaceChromeBinding(content, scroll, progress, chrome, scrollActions)
    SpacePaginationEffects(runtime, scroll, contentActions)
    SpacePagerSyncEffects(content, scroll, scrollActions)
    val actions = SpaceScreenActions(context, scrollActions::selectTab, contentActions::loadCollection,
        contentActions::loadArticles, contentActions::follow, scrollActions::applyPageDrag)
    // 空间页也会渲染动态卡片，所以同样需要页面级查看器宿主，
    // 否则点缩略图时 LocalPhotoViewer 会取不到值。
    val photo = remember { PhotoViewerStateHolder() }
    CompositionLocalProvider(LocalPhotoViewer provides photo) {
        PhotoViewerHost(state = photo, modifier = Modifier.fillMaxSize()) {
            SpaceScreenBody(content, scroll.layout, actions)
        }
    }
}
