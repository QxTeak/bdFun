package com.aritxonly.bdfun.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp

data class VideoActionIcons(val like: Int, val dislike: Int, val coin: Int, val favorite: Int, val share: Int)
data class VideoMetadataAppearance(val accentColor: Color, val icons: VideoActionIcons)

/** 缓存键沿用原标题映射，保留原remember失效边界，注解与导航策略由app装配。 */
class VideoDescriptionLinks(val cacheKey: Any?, val annotate: (String) -> AnnotatedString)

/** 只提供既有图片/图标/链接策略及窗口，不持有业务状态或另一个Activity实例。 */
class VideoMetadataSlots(
    val avatar: @Composable (String, Modifier) -> Unit,
    val metric: @Composable (Boolean, Color, Dp) -> Unit,
    val links: @Composable (String, Color) -> VideoDescriptionLinks,
    val copyDescription: (String) -> Unit,
    val activity: Activity?,
)
