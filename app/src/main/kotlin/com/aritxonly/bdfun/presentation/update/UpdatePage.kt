/* Adapted from Deadliner (MIT), Copyright (c) 2024 Aritx Zhou.
 * See docs/SETTINGS_DEADLINER_PROVENANCE.md. */
package com.aritxonly.bdfun.presentation.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.domain.update.*
import com.aritxonly.bdfun.feature.update.application.UpdateUiState
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.material.TeakGlassButton
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import androidx.compose.runtime.CompositionLocalProvider
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun UpdatePage(current: String, state: UpdateUiState, download: String,
    onCheck: () -> Unit, onDownload: () -> Unit, onHistory: () -> Unit, onBrowser: (String) -> Unit,
    animationsEnabled: Boolean = true) {
    AnimatedContent(targetState = state.history, modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (animationsEnabled) {
                val direction = if (targetState) 1 else -1
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { direction * it / 10 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { -direction * it / 10 })
            } else EnterTransition.None togetherWith ExitTransition.None
        }, label = "updateChangelogNavigation") { showLogs ->
        if (showLogs) com.aritxonly.bdfun.ui.ChangelogContent(state.releases, state.checking, state.error, onCheck)
        else UpdateCheckContent(current, state.copy(history = false), download, onCheck, onDownload, onHistory, onBrowser)
    }
}

@Composable
private fun UpdateCheckContent(current: String, state: UpdateUiState, download: String,
    onCheck: () -> Unit, onDownload: () -> Unit, onHistory: () -> Unit, onBrowser: (String) -> Unit) {
    val scroll = rememberScrollState()
    TrackTeakPageScroll(scroll)
    val surface = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop { drawRect(surface); drawContent() }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().layerBackdrop(backdrop).verticalScroll(scroll)
            .padding(horizontal = 24.dp)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 170.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            UpdateVersionHeader(current, state)
            if (state.release != null) TextButton(text = "更新日志", onClick = onHistory,
                colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = MiuixTheme.colorScheme.primary))
            if (state.error != null) Text(state.error)
            if (state.release == null && !state.checking) {
                if (state.currentRelease != null) UpdateNotesCard(state.currentRelease, current.removeSuffix("-debug"), onBrowser)
                else Text("当前版本暂无更新日志", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        CompositionLocalProvider(LocalTeakBackdrop provides backdrop) {
            UpdateBottomActions(state, download, onCheck, onDownload, onHistory,
                Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun UpdateNotesCard(release: UpdateRelease, current: String, onBrowser: (String) -> Unit) {
    val lines = remember(release.notes) { releaseNoteLines(release.notes) }
    Column(Modifier.fillMaxWidth().background(MiuixTheme.colorScheme.surfaceContainer,
        RoundedCornerShape(20.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(release.version.name + if (release.version.name == current) " · 当前版本" else "",
                modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            TextButton(text = "发布页", onClick = { onBrowser(release.page) },
                colors = ButtonDefaults.textButtonColors(color = Color.Transparent,
                    textColor = MiuixTheme.colorScheme.primary),
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp))
        }
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (lines.isEmpty()) Text("此版本暂无更新日志") else lines.forEach { Text(it) }
            }
        }
    }
}

@Composable
private fun UpdateBottomActions(state: UpdateUiState, download: String, onCheck: () -> Unit,
    onDownload: () -> Unit, onHistory: () -> Unit, modifier: Modifier) {
    val colors = MiuixTheme.colorScheme
    // 检查时按钮会禁用；库默认禁用色为蓝色，必须跟随本页主色。
    val primaryButtons = ButtonDefaults.buttonColorsPrimary(
        color = colors.primary, disabledColor = colors.primary.copy(alpha = .38f),
        contentColor = colors.onPrimary, disabledContentColor = colors.onPrimary.copy(alpha = .7f))
    Column(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(
        colors.surface.copy(alpha = 0f), colors.surface.copy(alpha = .65f), colors.surface.copy(alpha = .96f))))
        .navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (download.isNotBlank()) Text(download, fontSize = 13.sp)
        if (state.release != null && !state.history) {
            TeakGlassButton(onClick = onDownload, prominent = true, capsule = true, colors = primaryButtons,
                enabled = state.release.apk != null, modifier = Modifier.fillMaxWidth(), minHeight = 48.dp) {
                Text(if (state.release.apk != null) "下载更新" else "此版本暂无 APK", color = colors.onPrimary)
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TeakGlassButton(onClick = onCheck, enabled = !state.checking, prominent = true, colors = primaryButtons,
                capsule = true, minHeight = 48.dp, modifier = Modifier.weight(1f)) {
                Text(if (state.checking) "正在检查…" else "检查更新", color = colors.onPrimary)
            }
            TeakGlassButton(onClick = onHistory, capsule = true, minHeight = 48.dp, modifier = Modifier.weight(1f)) {
                Text(if (state.history) "返回检查" else "更新日志", color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun UpdateVersionHeader(current: String, state: UpdateUiState) {
    val colors = MiuixTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(buildAnnotatedString {
            append("bd")
            withStyle(SpanStyle(color = colors.primary)) { append("Fun") }
        }, fontSize = 52.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = colors.onSurface, modifier = Modifier.padding(vertical = 16.dp))
        Spacer(Modifier.height(24.dp))
        Text(state.release?.version?.name ?: current.removeSuffix("-debug"), fontSize = 16.sp,
            color = colors.onSurfaceVariantSummary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(when {
            state.checking -> "正在检查更新…"
            state.error != null -> "检查更新失败"
            state.release != null -> "发现新版本 · 当前版本 ${current.removeSuffix("-debug")}"
            state.checked -> "已是最新版本"
            else -> "检查更新"
        }, fontSize = 13.sp, color = colors.onSurfaceVariantSummary, textAlign = TextAlign.Center)
    }
}
