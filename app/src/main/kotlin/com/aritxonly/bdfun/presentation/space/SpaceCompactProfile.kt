package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SpaceCompactProfile(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val colors = MiuixTheme.colorScheme
    val ui = content.ui
    val banner = content.banner
    val pagePosition = content.pagePosition
    Column(modifier = Modifier.fillMaxWidth()) {
        SpaceBanner(bitmap = banner.bitmap)
        // 白色面板从头顶边开始，不做圆角矩形；头像用负 offset 盖到头上。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(top = 12.dp, bottom = 0.dp),
        ) {
            SpaceAvatarStats(content, actions)

            SpaceProfileDetails(content, actions)
            // 分段栏作为资料区的一部分，跟着内容一起上下滚（竖屏旧逻辑）。
            // 宽屏用的是顶栏那一份，这里只在竖屏出现。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                TeakPagerTabs(
                    options = listOf("主页", "动态", "投稿", "小店"),
                    selected = ui.selectedTab,
                    onSelect = actions.onSelectTab,
                    position = pagePosition,
                    // 这一份在页面内容里（会被录进页面 backdrop），不能再采样它
                    backdrop = null,
                )
            }
        }
    }
}
