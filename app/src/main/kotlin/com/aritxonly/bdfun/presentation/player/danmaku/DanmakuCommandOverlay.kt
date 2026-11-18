package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.domain.DanmakuCommandSelection
import com.aritxonly.bdfun.core.domain.activeDanmakuCommand
import com.aritxonly.bdfun.core.model.video.DanmakuCommand
import com.aritxonly.bdfun.core.model.video.DanmakuCommandKind
import com.aritxonly.bdfun.navigateToVideoByBvid
import kotlinx.coroutines.launch
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun BoxScope.DanmakuCommandOverlay(playback: PlaybackController, aid: Long, cid: Long, actions: VideoInteractionActions, videoLiked: Boolean) {
    val controller = LocalVideoPageState.current.danmakuModeration ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var commands by remember(aid, cid) { mutableStateOf(emptyList<DanmakuCommand>()) }
    var dismissed by remember(aid, cid) { mutableStateOf(emptySet<Long>()) }
    var selections by remember(aid, cid) { mutableStateOf(emptyMap<Long, Int>()) }
    var pending by remember(aid, cid) { mutableStateOf<Long?>(null) }
    var link by remember(aid, cid) { mutableStateOf<DanmakuCommand?>(null) }
    LaunchedEffect(controller, aid, cid) { if (aid > 0 && cid > 0) commands = controller.commands(aid, cid) }
    val playbackState = playback.state.collectAsState()
    val active by remember(commands, dismissed, playbackState) {
        derivedStateOf { activeDanmakuCommand(commands, dismissed, playbackState.value.positionMs) }
    }
    active?.let { command ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cardWidth = (maxWidth - 24.dp).coerceIn(1.dp, 292.dp)
            val x = command.x?.let { (maxWidth * it - cardWidth / 2).coerceIn(0.dp, (maxWidth - cardWidth).coerceAtLeast(0.dp)) }
            val y = command.y?.let { (maxHeight * it).coerceIn(0.dp, (maxHeight - 128.dp).coerceAtLeast(0.dp)) }
            val position = if (x != null && y != null) Modifier.align(Alignment.TopStart).offset(x, y)
                else Modifier.align(Alignment.BottomCenter).padding(bottom = 52.dp)
            DanmakuCommandCard(command, selections[command.id] ?: command.selected, pending != null,
                position.width(cardWidth), actions, videoLiked, onLink = { link = command },
                onDismiss = { dismissed = dismissed + command.id }) { value ->
                val request = DanmakuCommandSelection(aid, cid, command, value)
                if (!request.valid || pending != null) return@DanmakuCommandCard
                pending = command.id
                scope.launch {
                    try {
                        if (controller.submitCommand(request)) {
                            selections = selections + (command.id to value)
                            if (command.kind == DanmakuCommandKind.Grade) {
                                val latest = controller.commands(aid, cid).firstOrNull { it.interactionId == command.interactionId && it.kind == command.kind }
                                if (latest != null) commands = commands.map { if (it.id == command.id) latest.copy(id = command.id) else it }
                            }
                        } else context.showToast("提交失败，请稍后重试")
                    } finally { pending = null }
                }
            }
        }
    }
    link?.let { target ->
        WindowDialog(show = true, title = "打开关联视频", summary = target.text, onDismissRequest = { link = null }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { link = null }, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(onClick = {
                    val bvid = target.linkBvid
                    link = null
                    if (bvid.isBlank()) context.showToast("关联视频信息缺失")
                    else navigateToVideoByBvid(context, bvid)
                }, modifier = Modifier.weight(1f)) { Text("打开") }
            }
        }
    }
}

@Composable
private fun DanmakuCommandCard(
    command: DanmakuCommand, selected: Int?, busy: Boolean, modifier: Modifier, actions: VideoInteractionActions, videoLiked: Boolean,
    onLink: () -> Unit, onDismiss: () -> Unit, onSubmit: (Int) -> Unit,
) {
    val color = MiuixTheme.colorScheme.onBackground
    Card(modifier = modifier) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(command.text, color = color, modifier = Modifier.weight(1f), maxLines = 2)
                IconButton(onClick = onDismiss) { AppGlyph(MiuixIcons.Close, color, size = 18.dp, contentDescription = "关闭") }
            }
            when (command.kind) {
                DanmakuCommandKind.Notice -> Unit
                DanmakuCommandKind.Link -> IconButton(onClick = onLink) { AppGlyph(MiuixIcons.Link, color, size = 18.dp, contentDescription = "打开关联视频") }
                DanmakuCommandKind.Follow -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (command.attentionType in 1..2) {
                        IconButton(onClick = actions.onLikeClick) { AppGlyph(TeakIcon.like(videoLiked), if (videoLiked) MiuixTheme.colorScheme.primary else color,
                            size = 18.dp, contentDescription = if (videoLiked) "取消点赞视频" else "点赞视频") }
                        IconButton(onClick = actions.onCommandCoin) { AppGlyph(LucideIcons.CoinsFilled, color, size = 18.dp, contentDescription = "投币") }
                        IconButton(onClick = actions.onCommandFavorite) { AppGlyph(LucideIcons.Star, color, size = 18.dp, contentDescription = "收藏") }
                    }
                    if (command.attentionType != 1) IconButton(onClick = actions.onCommandFollow) { AppGlyph(MiuixIcons.Add, color, size = 18.dp, contentDescription = "关注 UP 主") }
                }
                DanmakuCommandKind.Grade, DanmakuCommandKind.Vote -> {
                    command.options.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { option ->
                                Button(onClick = { onSubmit(option.value) }, enabled = !busy && selected == null && command.interactionId > 0,
                                    modifier = Modifier.weight(1f)) {
                                    Text(if (selected == option.value) "✓ ${option.label}" else option.label, maxLines = 1)
                                }
                            }
                        }
                    }
                    if (command.kind == DanmakuCommandKind.Grade) {
                        val summary = listOfNotNull(command.average?.let { "平均 ${String.format(java.util.Locale.ROOT, "%.1f", it)} 分" },
                            command.participants?.let { "${it} 人参与" }).joinToString(" · ")
                        if (summary.isNotEmpty()) Text(summary, color = color)
                    }
                }
            }
        }
    }
}
