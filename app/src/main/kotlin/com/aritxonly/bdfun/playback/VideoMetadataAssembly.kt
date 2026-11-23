package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.navigateToVideoByBvid

internal class VideoMetadataAssembly(val appearance: VideoMetadataAppearance, val slots: VideoMetadataSlots)

/** 图片、资源及链接策略沿用app实现，视频模块只负责原有正文布局与交互。 */
@Composable
internal fun rememberVideoMetadataAssembly(): VideoMetadataAssembly {
    val context = LocalContext.current
    val icons = remember {
        VideoActionIcons(TeakIcon.like(true), TeakIcon.Dislike, TeakIcon.Coin, TeakIcon.Star, TeakIcon.Share)
    }
    val slots = remember(context) {
        VideoMetadataSlots(
            avatar = { face, modifier ->
                AsyncImage(model = rememberImageRequest(face), contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = modifier)
            },
            metric = { play, color, size -> MetricGlyph(play = play, color = color, size = size) },
            links = { text, linkColor ->
                val titles = rememberBiliLinkTitles(text)
                val linkContext = LocalContext.current
                val uriHandler = LocalUriHandler.current
                VideoDescriptionLinks(titles) { segment ->
                    linkifyText(segment, linkColor, titles,
                        onOpenVideo = { navigateToVideoByBvid(linkContext, it) },
                        onOpen = { uriHandler.openUri(it) })
                }
            },
            copyDescription = { text ->
                context.copyToClipboard("简介", text)
                context.showToast("已复制简介")
            },
            copyText = { text ->
                context.copyToClipboard("BV 号", text)
                context.showToast("已复制")
            },
            activity = context.findHostActivity(),
        )
    }
    return VideoMetadataAssembly(VideoMetadataAppearance(BrandPink, icons), slots)
}
