package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.ui.EmojiPanel
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.teakClassicBarGlass
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassRecipes
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PrivateChatComposer(content: PrivateChatComposerContent, actions: PrivateChatComposerActions) {
    val showEmoji = content.showEmoji
    val uploading = content.uploading
    val onPickEmoji = actions.onPickEmoji
    val colors = MiuixTheme.colorScheme
    val recipe = TeakGlassRecipes.floatingNavigation(LocalTeakMaterialSpec.current.fineTuning)
    val backdrop = LocalTeakBackdrop.current
    val material = PrivateChatComposerMaterial(recipe, backdrop)
    val density = LocalDensity.current
    val imeDp = with(density) {
        WindowInsets.ime.getBottom(density).toDp().value
    }
    val navDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value
    val raiseDp = if (AppGraph.settings.bottomBarRaise) {
        TeakChromeDefaults.RaisedBottomBarExtra.value
    } else {
        0f
    }
    val animatedBottom = if (imeDp > navDp) imeDp else navDp + raiseDp

    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    val barColor = colors.surfaceContainer
    val dividerColor = colors.onBackground.copy(alpha = 0.12f)

    Box(Modifier.fillMaxWidth()) {
        if (classic) {
            // 输入栏整条玻璃模糊 + 半透明底，保证输入框文字始终清晰。
            Box(Modifier.matchParentSize().teakClassicBarGlass())
            Box(Modifier.matchParentSize().background(barColor.copy(alpha = 0.62f)))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = animatedBottom.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (showEmoji) {
                EmojiPanel(
                    onPick = onPickEmoji,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            PrivateChatComposerRow(content, actions, material)
            if (uploading) {
                Text(
                    text = "图片上传中…",
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 5.dp),
                )
            }
        }
    }
}
