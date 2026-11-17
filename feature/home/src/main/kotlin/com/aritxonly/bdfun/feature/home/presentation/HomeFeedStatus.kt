package com.aritxonly.bdfun.feature.home.presentation

import com.aritxonly.teak.common.chrome.TeakGlass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.HomeFeedStatus(
    state: HomeFeedState, scope: kotlinx.coroutines.CoroutineScope, host: HomePresentationHost,
) {
    val colors = MiuixTheme.colorScheme
        // 首屏加载已经由网格里的骨架屏承担，这里不再叠一个转圈。

        state.error?.let { message ->
            if (state.videos.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = message, color = colors.onBackgroundVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    TeakGlass(
                        modifier = Modifier.height(34.dp),
                        shape = Capsule(),
                        onClick = { scope.launch { state.refresh() } },
                    ) {
                        Text(
                            text = "重试",
                            color = colors.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 18.dp).align(Alignment.Center),
                        )
                    }
                }
            }
        }
}
