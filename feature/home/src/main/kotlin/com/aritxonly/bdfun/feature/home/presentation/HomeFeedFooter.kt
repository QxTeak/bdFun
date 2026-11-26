package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakLoadingIndicator
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun HomeFeedFooter(state: HomeFeedState, scope: kotlinx.coroutines.CoroutineScope) {
    val colors = MiuixTheme.colorScheme
    val footerError = state.loadMoreError
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 加载态比普通文字高，固定一个最小高度，避免出现/收起时把列表顶一下。
            .heightIn(min = 64.dp)
            .padding(vertical = 10.dp)
            .then(
                if (footerError != null) {
                    Modifier.clickable { scope.launch { state.loadMore() } }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state.loadingMore) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TeakLoadingIndicator()
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "正在刷新…",
                    color = colors.onBackgroundVariant,
                    fontSize = 12.sp,
                )
            }
        } else {
            Text(
                text = when {
                    footerError != null -> footerError
                    state.endReached -> "没有更多了"
                    else -> "上滑加载更多"
                },
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
            )
        }
    }
}
