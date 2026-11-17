package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.teakClassicBarGlass
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentComposerBody(
    content: CommentComposerContent, actions: CommentComposerActions,
    material: CommentComposerMaterial, modifier: Modifier,
) {
    val showEmoji = content.snapshot.showEmoji
    val uploading = content.snapshot.uploading
    val failed = content.snapshot.failed
    val colors = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeDp = with(density) { imeBottom.toDp().value }
    val navDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value
    val raiseDp = if (com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings.current.bottomBarRaise) {
        TeakChromeDefaults.RaisedBottomBarExtra.value
    } else {
        0f
    }
    // 跟随系统 IME inset（不做自定义动画）；未开导航条时按设置额外抬高。
    val animatedBottom = if (imeDp > navDp) imeDp else navDp + raiseDp

    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    val barColor = colors.surfaceContainer
    val dividerColor = colors.onBackground.copy(alpha = 0.12f)

    Box(modifier = modifier.fillMaxWidth()) {
        if (classic) {
            // 输入栏整条玻璃模糊 + 半透明底，保证输入框文字始终清晰。
            Box(Modifier.matchParentSize().teakClassicBarGlass())
            Box(Modifier.matchParentSize().background(barColor.copy(alpha = 0.62f)))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = animatedBottom.dp, start = 12.dp, end = 12.dp)
                .padding(vertical = 8.dp),
        ) {
            if (showEmoji) {
                EmojiPanel(
                    onPick = actions::pickEmoji,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            CommentComposerRow(content, actions, material)
            if (failed || uploading) {
                Text(
                    text = if (uploading) "图片上传中…" else "发送失败，请确认已登录后重试",
                    color = if (uploading) colors.onBackgroundVariant else MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 5.dp),
                )
            }
        }
    }
}
