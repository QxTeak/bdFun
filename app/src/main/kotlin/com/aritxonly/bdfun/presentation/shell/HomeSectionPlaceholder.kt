package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 仅用于新入口交互验收，不能展示其它频道的数据冒充内容。 */
@Composable
internal fun HomeSectionPlaceholder(title: String, topBarHeight: Dp) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = topBarHeight),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = MiuixTheme.colorScheme.onBackground)
        Text("内容即将上线", color = MiuixTheme.colorScheme.onBackgroundVariant)
    }
}
