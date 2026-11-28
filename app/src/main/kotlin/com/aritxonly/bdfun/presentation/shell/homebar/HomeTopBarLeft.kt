package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TeakGlassBackButton

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp


@Composable
internal fun HomeTopBarLeft(content: HomeTopBarContent, search: HomeTopBarSearch, actions: HomeTopBarActions) {
    val searchActive = search.active
    val profile = content.profile
    val onCancelSearch = actions.onCancelSearch
    val onOpenProfile = actions.onOpenProfile
    Crossfade(
        targetState = searchActive,
        animationSpec = tween(durationMillis = 160),
        label = "left-slot",
    ) { searching ->
        if (searching) {
            // 搜索态返回键与 TeakUI 统一页面完全同一套：尺寸、玻璃和 12dp 左边距都走默认值。
            // 不能在这里塞 38dp 的小按钮，否则和设置/收藏等页面并排时明显小一圈。
            TeakGlassBackButton(
                onClick = onCancelSearch,
                revealed = true,
            )
        } else {
            TeakAvatar(faceUrl = profile?.face, onClick = onOpenProfile, size = 38.dp)
        }
    }
}
