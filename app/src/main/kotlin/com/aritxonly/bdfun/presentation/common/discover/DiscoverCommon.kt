package com.aritxonly.bdfun.presentation.common.discover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakGlass
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 直播 / 番剧两段共用的小节标题。
 *
 * 字号/字重和番剧页榜单分节标题（`BangumiSectionTitle`）完全一致（20sp / Bold / 24sp 行高），
 * 全站只保留这一份：不要再各自的页面里另写小标题样式，否则一定会漂。
 */
@Composable
internal fun DiscoverSectionTitle(text: String) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onBackground,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
    )
}

/** 直播 / 番剧两段共用的「空态 + 重试」。 */
@Composable
internal fun DiscoverErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        TeakGlass(modifier = Modifier.height(34.dp), shape = Capsule(), onClick = onRetry) {
            Text(
                text = "重试",
                color = MiuixTheme.colorScheme.primary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 18.dp).align(Alignment.Center),
            )
        }
    }
}
