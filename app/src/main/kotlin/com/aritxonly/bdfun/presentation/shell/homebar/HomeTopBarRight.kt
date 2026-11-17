package com.aritxonly.bdfun.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Messages

@Composable
internal fun HomeTopBarRight(content: HomeTopBarContent, search: HomeTopBarSearch,
    actions: HomeTopBarActions, material: HomeTopBarMaterial) {
    val searchActive = search.active
    val unreadCount = content.unreadCount
    val onClearQuery = actions.onClearQuery
    val onOpenMessages = actions.onOpenMessages
    val recipe = material.recipe
    val backdrop = material.backdrop
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    Crossfade(
        targetState = searchActive,
        animationSpec = tween(durationMillis = 160),
        label = "right-slot",
    ) { searching ->
        if (searching) {
            if (classic) {
                // 经典样式：顶栏动作只保留图标，不加玻璃圆底。
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(interactionSource = null, indication = null, onClick = onClearQuery),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MiuixIcons.Close, contentDescription = "清空搜索", tint = BrandPink,
                        modifier = Modifier.size(18.dp))
                }
            } else {
                TeakGlassIconButton(
                    materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                    onClick = onClearQuery,
                    size = 40.dp,
                    recipe = recipe,
                    backdrop = backdrop,
                ) {
                    Icon(MiuixIcons.Close, contentDescription = "清空搜索", tint = BrandPink,
                        modifier = Modifier.size(18.dp))
                }
            }
        } else {
            if (classic) {
                UnreadBadgeHost(count = unreadCount) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(interactionSource = null, indication = null, onClick = onOpenMessages),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(MiuixIcons.Messages, contentDescription = "消息", tint = BrandPink,
                            modifier = Modifier.size(19.dp))
                    }
                }
            } else {
                UnreadBadgeHost(count = unreadCount) {
                    TeakGlassIconButton(
                        materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                        flatBackgroundAlpha = com.aritxonly.teak.common.material.TeakGlassDefaults.flatBackgroundAlpha(),
                        onClick = onOpenMessages, size = 40.dp, recipe = recipe, backdrop = backdrop,
                    ) {
                        Icon(MiuixIcons.Messages, contentDescription = "消息", tint = BrandPink,
                            modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
    }
}
