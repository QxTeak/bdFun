package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.ui.*
import com.aritxonly.teak.common.chrome.*
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.aritxonly.bdfun.feature.relations.application.*

@Composable
internal fun RelationFooter(controller: RelationFeedStateHolder, state: RelationFeedState, fans: Boolean,
    scope: kotlinx.coroutines.CoroutineScope) {
    val colors = MiuixTheme.colorScheme
    LaunchedEffect(state.loading, state.error, state.nextPage) {
        if (!state.loading && state.error == null && state.nextPage != null) scope.launch { controller.fetch() }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.loading) {
            MiuixLoadingRow(verticalPadding = 0.dp)
            return@Column
        }
        Text(when {
            state.error != null -> "${state.error}\n点击重试"
            state.items.isEmpty() -> if (fans) "还没有粉丝" else "还没有关注的人"
            state.nextPage != null -> "加载更多"
            else -> "已显示全部可见用户"
        }, color = colors.onBackgroundVariant, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable(enabled = !state.loading &&
                (state.error != null || state.nextPage != null)) {
                scope.launch { if (state.error != null) controller.retry() else controller.fetch() }
            }.padding(12.dp))
    }
}
