package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import com.aritxonly.bdfun.feature.settings.application.*
import androidx.compose.runtime.collectAsState

internal class DanmakuPanelRuntime(
    val settings: SettingsBindings, val controller: DanmakuPanelStateHolder,
    val ui: DanmakuPanelUiState, val fullscreen: Boolean, val onInteract: () -> Unit, val items: List<DanmakuItem>,
    val liveMode: Boolean,
)

@Composable
fun DanmakuSettingsContent(fullscreen: Boolean, onInteract: () -> Unit,
    danmakuItems: List<DanmakuItem> = emptyList(), modifier: Modifier = Modifier, liveMode: Boolean = false,
    videoTools: @Composable () -> Unit = {},
) {
    val settings = LocalSettingsBindings.current
    val controller = remember(settings) { DanmakuPanelStateHolder(settings.repository.danmaku) }
    val ui by controller.state.collectAsState()
    val runtime = DanmakuPanelRuntime(settings, controller, ui, fullscreen, onInteract, danmakuItems, liveMode)
    Column(modifier = modifier.fillMaxWidth()) {
        DanmakuPanelSliders(runtime)
        Spacer(Modifier.height(8.dp))
        DanmakuPanelFilters(runtime)
        Spacer(Modifier.height(8.dp))
        DanmakuPanelTools(runtime)
        videoTools()
        Spacer(Modifier.height(8.dp))
        DanmakuPanelReset(runtime)
        Spacer(Modifier.height(4.dp))
    }
}
