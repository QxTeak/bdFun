package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** 资源归组装壳所有；视频展示只使用传入ID，不反向引用app的R或全局主题。 */
data class FullscreenIconAssets(
    val play: Int, val next: Int, val danmakuOn: Int, val danmakuOff: Int, val danmakuSettings: Int,
    val like: (Boolean) -> Int, val comment: Int, val favorite: Int, val share: Int,
)
data class FullscreenAppearance(
    val accentColor: Color, val icons: FullscreenIconAssets,
    val showActions: Boolean, val showViewerCount: Boolean, val showTime: Boolean,
)

/** 图片请求和跨设置/视频/直播共用的设置内容由app组装，视频布局与点击归本模块。 */
class FullscreenChromeSlots(
    val avatar: @Composable (String, Modifier) -> Unit,
    val cover: @Composable (String, Modifier) -> Unit,
    val danmakuSettings: @Composable (List<com.aritxonly.bdfun.core.model.video.DanmakuItem>) -> Unit,
    /** 全屏内评论面板：列表由 app 侧的真实评论页提供，模块只负责面板外壳并传入当前视频身份。 */
    val comments: @Composable (
        info: FullscreenPlayerInfo,
        /** 面板实际宽度：评论楼中楼是独立窗口，要按同一个宽度对齐才不会错位。 */
        paneWidth: androidx.compose.ui.unit.Dp,
        Modifier,
    ) -> Unit = { _, _, _ -> },
    /** 评论面板底部回复栏；只在评论面板展开时挂一次，避免和页面级回复栏重复。 */
    val commentComposer: @Composable (Modifier) -> Unit = {},
)
