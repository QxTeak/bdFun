package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import com.aritxonly.teak.common.theme.TeakUiStyle
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.theme.TeakTheme
import com.aritxonly.bdfun.data.SettingsStore

@Composable
fun BdFunApp(settings: SettingsStore) {
    com.aritxonly.bdfun.ui.PageChromeScope {
        val theme = rememberMainShellTheme(settings)
        val shell = rememberMainShellState(theme.materialColors.background)
        SideEffect { TeakUiStyle.style = settings.interfaceStyle }
        TeakTheme(
            dark = theme.dark,
            lightColors = theme.light,
            darkColors = theme.darkColors,
            materialColors = theme.materialColors,
            blurEnabled = settings.blurEnabled,
            refractionEnabled = settings.refractionEnabled,
            fontScale = settings.fontScale,
            hapticsEnabled = settings.vibrationEnabled,
        ) {
            CompositionLocalProvider(LocalTeakBackdrop provides shell.backdrops.main,
                com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings provides settings,
                com.aritxonly.bdfun.ui.LocalRichTextDependencies provides AppGraph.richTextDependencies) {
                MainShellEffects(settings, shell)
                MainScaffold(settings, shell, theme.dark, theme.materialColors.background)
            }
        }
    }
}
