package com.aritxonly.bdfun.ui

import android.view.TextureView
import com.aritxonly.bdfun.core.application.InlinePlaybackController

/** 原生绑定由 app 容器装配，展示层只保留 TextureView 的布局。 */
internal data class InlinePlaybackRuntime(
    val controller: InlinePlaybackController,
    val bindSurface: (TextureView) -> Unit,
)
