package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.TeakPagerTabs

@Composable
internal fun MessageSessionTabs(ui: MessageSessionsPresentationStateHolder) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        TeakPagerTabs(
            options = listOf("全部", "我关注的人"), selected = ui.tabIndex,
            onSelect = { ui.tabIndex = it }, itemWidth = 96.dp,
            backdrop = null, plain = false,
        )
    }
}
