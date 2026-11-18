package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink

internal class FullscreenChromeAssembly(val appearance: FullscreenAppearance, val slots: FullscreenChromeSlots)

/** 原app资源、图片请求与共用设置展示只在组装壳接线，不进入视频模块。 */
@Composable
internal fun rememberFullscreenChromeAssembly(): FullscreenChromeAssembly {
    val settings = LocalVideoPageState.current.settings
    val icons = remember {
        FullscreenIconAssets(TeakIcon.PlayerPlay, TeakIcon.PlayerNext, TeakIcon.PlayerDanmakuOn,
            TeakIcon.PlayerDanmakuOff, TeakIcon.PlayerDanmakuSettings,
            TeakIcon::like, TeakIcon.PlayerComment, TeakIcon.PlayerStar, TeakIcon.PlayerShare)
    }
    val slots = remember {
        FullscreenChromeSlots(
            avatar = { face, modifier ->
                AsyncImage(model = rememberImageRequest(face), contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = modifier)
            },
            cover = { cover, modifier ->
                AsyncImage(model = rememberImageRequest(cover), contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = modifier)
            },
            danmakuSettings = { items ->
                DanmakuSettingsContent(fullscreen = true, onInteract = {}, danmakuItems = items)
            },
        )
    }
    return FullscreenChromeAssembly(FullscreenAppearance(BrandPink, icons,
        settings.showFullscreenActions, settings.showViewerCount, settings.showFullscreenTime), slots)
}
