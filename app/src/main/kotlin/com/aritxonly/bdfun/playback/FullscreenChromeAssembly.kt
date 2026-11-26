package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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
                DanmakuSettingsContent(fullscreen = true, onInteract = {}, danmakuItems = items) {
                    DanmakuCloudRulesEntry(onInteract = {})
                }
            },
            comments = { info, paneWidth, modifier ->
                // 全屏评论面板复用详情页那套评论页；这里必须换 key，
                // 换集/换稿件时评论列表、楼中楼都要跟着重建。
                val comments = info.comments
                androidx.compose.runtime.key(comments.aid, comments.bvid) {
                    // 面板只有四百多 dp 宽，评论页的「评论 N」大标题和 96dp 底部留白在这儿只会挤掉正文。
                    androidx.compose.foundation.layout.Box(modifier) {
                        CommentScreen(
                            request = CommentScreenRequest(
                                aid = comments.aid,
                                upMid = comments.upMid,
                                title = info.title,
                                bvid = comments.bvid,
                                commentOid = comments.aid.toString(),
                                commentType = 1,
                            ),
                            onBack = {},
                            layout = CommentScreenLayout(
                                bare = true, topInset = 6.dp, bottomPadding = 20.dp,
                                flatList = true,
                                // 面板右侧贴着刘海/挖孔：默认的状态栏 padding 正好把首条评论让出来。
                                immersiveStatusBar = false,
                                // 楼中楼是独立窗口，顶边默认读「播放器底边」；全屏时那是整屏高度，
                                // 面板会被压成 1px 高，表现为点了没反应。这里钉死从屏幕顶开始、宽同面板。
                                threadPlayerBottom = 0.dp,
                                threadPaneWidth = paneWidth,
                            ),
                        )
                    }
                }
            },
            commentComposer = { modifier -> CommentReplyBar(modifier = modifier) },
        )
    }
    return FullscreenChromeAssembly(FullscreenAppearance(BrandPink, icons,
        settings.showFullscreenActions, settings.showViewerCount, settings.showFullscreenTime), slots)
}
