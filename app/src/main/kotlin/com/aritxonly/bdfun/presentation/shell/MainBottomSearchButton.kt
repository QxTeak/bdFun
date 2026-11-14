package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search

@Composable
internal fun MainBottomSearchButton(alpha: Float, onClick: () -> Unit) {
    TeakGlassIconButton(onClick = onClick, size = TeakFloatingTabBarDefaults.CurrentHeight,
        modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
        Icon(MiuixIcons.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(TeakFloatingTabBarDefaults.DetachedIconSize))
    }
}
