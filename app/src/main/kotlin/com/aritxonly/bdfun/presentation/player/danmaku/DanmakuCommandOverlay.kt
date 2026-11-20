package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.domain.DanmakuCommandSelection
import com.aritxonly.bdfun.core.domain.activeDanmakuCommand
import com.aritxonly.bdfun.core.model.video.DanmakuCommand
import com.aritxonly.bdfun.core.model.video.DanmakuCommandKind
import com.aritxonly.bdfun.navigateToVideoByBvid
import kotlinx.coroutines.launch
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 视频互动：弹幕里的投票 / 评分 / 关注 / 跳转指令。
 *
 * 面板沿用「选中弹幕」那套黑色半透明玻璃（68% 黑 + 22% 白描边），
 * 不用系统 Card 的大色块；面板最大占播放器面积的一半，并且始终留在画面内。
 */
@Composable
internal fun BoxScope.DanmakuCommandOverlay(
    playback: PlaybackController, aid: Long, cid: Long,
    actions: VideoInteractionActions, videoLiked: Boolean,
) {
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
            // 最多占播放器宽度 / 高度的一半，保证面板不会盖住大半个画面。
            val cardWidth = (maxWidth * 0.5f).coerceAtMost(320.dp).coerceAtLeast(1.dp)
            val cardMaxHeight = (maxHeight * 0.5f).coerceAtLeast(1.dp)
            val x = command.x?.let {
                (maxWidth * it - cardWidth / 2).coerceIn(0.dp, (maxWidth - cardWidth).coerceAtLeast(0.dp))
            }
            val y = command.y?.let {
                (maxHeight * it).coerceIn(0.dp, cardMaxHeight)
            }
            val position = if (x != null && y != null) {
                Modifier.align(Alignment.TopStart).offset(x, y)
            } else {
                Modifier.align(Alignment.BottomCenter).padding(bottom = 52.dp)
            }
            DanmakuCommandCard(
                command, selections[command.id] ?: command.selected, pending != null,
                position.width(cardWidth).heightIn(max = cardMaxHeight),
                actions, videoLiked,
                onLink = { link = command },
                onDismiss = { dismissed = dismissed + command.id },
            ) { value ->
                val request = DanmakuCommandSelection(aid, cid, command, value)
                if (!request.valid || pending != null) return@DanmakuCommandCard
                pending = command.id
                scope.launch {
                    try {
                        if (controller.submitCommand(request)) {
                            selections = selections + (command.id to value)
                            if (command.kind == DanmakuCommandKind.Grade) {
                                val latest = controller.commands(aid, cid)
                                    .firstOrNull { it.interactionId == command.interactionId && it.kind == command.kind }
                                if (latest != null) {
                                    commands = commands.map { if (it.id == command.id) latest.copy(id = command.id) else it }
                                }
                            }
                        } else {
                            context.showToast("提交失败，请稍后重试")
                        }
                    } finally {
                        pending = null
                    }
                }
            }
        }
    }
    link?.let { target ->
        WindowDialog(show = true, title = "打开关联视频", summary = target.text, onDismissRequest = { link = null }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiuixButton(onClick = { link = null }, modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                MiuixButton(onClick = {
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
    command: DanmakuCommand, selected: Int?, busy: Boolean, modifier: Modifier,
    actions: VideoInteractionActions, videoLiked: Boolean,
    onLink: () -> Unit, onDismiss: () -> Unit, onSubmit: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val contentColor = Color.White
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.68f))
            .border(0.5.dp, Color.White.copy(alpha = 0.22f), shape)
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = command.text,
                color = contentColor,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                AppGlyph(MiuixIcons.Close, contentColor.copy(alpha = 0.75f), size = 12.dp, contentDescription = "关闭")
            }
        }
        when (command.kind) {
            DanmakuCommandKind.Notice -> Unit
            DanmakuCommandKind.Link -> CapsuleButton("打开关联视频", enabled = true, onClick = onLink)
            DanmakuCommandKind.Follow -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (command.attentionType in 1..2) {
                        CapsuleIconButton(TeakIcon.like(videoLiked), if (videoLiked) MiuixTheme.colorScheme.primary else contentColor,
                            description = if (videoLiked) "取消点赞视频" else "点赞视频", onClick = actions.onLikeClick)
                        CapsuleIconButton(LucideIcons.CoinsFilled, contentColor, "投币", actions.onCommandCoin)
                        CapsuleIconButton(LucideIcons.Star, contentColor, "收藏", actions.onCommandFavorite)
                    }
                    if (command.attentionType != 1) {
                        CapsuleIconButton(MiuixIcons.Add, contentColor, "关注 UP 主", actions.onCommandFollow)
                    }
                }
            }
            DanmakuCommandKind.Grade, DanmakuCommandKind.Vote -> {
                command.options.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { option ->
                            CapsuleOption(
                                label = option.label,
                                selected = selected == option.value,
                                enabled = !busy && selected == null && command.interactionId > 0,
                                onClick = { onSubmit(option.value) },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (command.kind == DanmakuCommandKind.Grade) {
                    val summary = listOfNotNull(
                        command.average?.let { "平均 ${String.format(java.util.Locale.ROOT, "%.1f", it)} 分" },
                        command.participants?.let { "${it} 人参与" },
                    ).joinToString(" · ")
                    if (summary.isNotEmpty()) {
                        Text(summary, color = contentColor.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.CapsuleOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = Capsule()
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (selected) 0.95f else 0.42f),
                shape = shape,
            )
            .clickable(enabled = enabled, interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (selected) "✓ $label" else label,
            color = Color.White.copy(alpha = if (enabled || selected) 0.92f else 0.45f),
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CapsuleButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = Capsule()
    Box(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.6f else 0.3f), shape)
            .clickable(enabled = enabled, interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White.copy(alpha = if (enabled) 0.92f else 0.45f), fontSize = 11.5.sp, maxLines = 1)
    }
}

@Composable
private fun CapsuleIconButton(
    icon: Int,
    tint: Color,
    description: String,
    onClick: () -> Unit,
) {
    CapsuleIconBox(onClick = onClick) {
        AppGlyph(icon, tint, size = 15.dp, contentDescription = description)
    }
}

@Composable
private fun CapsuleIconButton(
    icon: ImageVector,
    tint: Color,
    description: String,
    onClick: () -> Unit,
) {
    CapsuleIconBox(onClick = onClick) {
        AppGlyph(icon, tint, size = 15.dp, contentDescription = description)
    }
}

@Composable
private fun CapsuleIconBox(onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = Capsule()
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(shape)
            .border(0.5.dp, Color.White.copy(alpha = 0.3f), shape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
