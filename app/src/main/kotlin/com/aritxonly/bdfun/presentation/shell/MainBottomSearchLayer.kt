package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.BottomSearchFrame
import com.aritxonly.bdfun.ui.BottomSearchTransition
import com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress
import com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial
import com.aritxonly.teak.common.material.LocalTeakBackdrop

@Composable
internal fun MainBottomSearchLayer(settings: SettingsBindings, shell: MainShellState) {
    val state = shell.bottom
    if (state.showBottomSearch || state.bottomSearchProgress.value > 0.001f) {
        BottomSearchFrame(BottomSearchTransition(state.bottomSearchProgress.value, state.bottomWindowAlpha.value,
            state.bottomMaskAlpha.value, state.bottomContentAlpha.value)) {
            Box(Modifier.fillMaxSize()) {
                MainSearchLayer(settings, shell, bottomSearch = true)
                CompositionLocalProvider(
                    LocalTeakBackdrop provides shell.backdrops.search,
                    LocalTopBarScrollProgress provides shell.chrome.search.materialProgress,
                ) {
                    if (!(state.bottomSubmitted != null && com.aritxonly.bdfun.ui.useSingleRowSearchResults())) {
                        ProgressiveTopBarMaterial { MainHomeBarContent(settings, shell, bottomSearch = true) }
                    }
                }
            }
        }
    }
}
