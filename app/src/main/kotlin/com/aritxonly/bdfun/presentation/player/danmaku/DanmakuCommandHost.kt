package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.domain.DanmakuCommandSelection
import com.aritxonly.bdfun.core.model.video.DanmakuCommand
import com.aritxonly.bdfun.core.model.video.DanmakuCommandKind
import com.aritxonly.bdfun.feature.video.application.DanmakuModerationStateHolder
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.math.roundToInt

/**
 * 互动指令的会话状态：拉取、关闭、选择、提交。
 *
 * 从 Overlay 中抽出来，避免一个 159 行的 Composable 同时管布局、状态和网络时序；
 * 提交顺序与原来完全一致（先占 pending，再提交，投票/评分成功后再刷新票数）。
 */
internal class DanmakuCommandSession(
    private val controller: DanmakuModerationStateHolder,
    private val aid: Long,
    private val cid: Long,
    private val scope: CoroutineScope,
    private val toast: (String) -> Unit,
) {
    var commands by mutableStateOf(emptyList<DanmakuCommand>())
        private set
    var dismissed by mutableStateOf(emptySet<Long>())
        private set
    var selections by mutableStateOf(emptyMap<Long, Int>())
        private set
    var pending by mutableStateOf<Long?>(null)
        private set
    var link by mutableStateOf<DanmakuCommand?>(null)
        private set

    suspend fun load() {
        if (aid > 0 && cid > 0) commands = controller.commands(aid, cid)
    }

    suspend fun voteCounts(interactionId: Long): Map<Int, Long> =
        if (interactionId > 0) controller.voteCounts(interactionId) else emptyMap()

    fun dismiss(id: Long) {
        dismissed = dismissed + id
    }

    fun openLink(command: DanmakuCommand) {
        link = command
    }

    fun closeLink() {
        link = null
    }

    fun submit(
        command: DanmakuCommand,
        value: Int,
        onShowRatio: () -> Unit,
        onVoteCounts: (Map<Int, Long>) -> Unit,
    ) {
        val request = DanmakuCommandSelection(aid, cid, command, value)
        if (!request.valid || pending != null) return
        pending = command.id
        scope.launch {
            try {
                if (!controller.submitCommand(request)) {
                    toast("提交失败，请稍后重试")
                    return@launch
                }
                selections = selections + (command.id to value)
                if (command.kind == DanmakuCommandKind.Vote) {
                    onShowRatio()
                    val counts = controller.voteCounts(command.interactionId)
                    if (counts.isNotEmpty()) {
                        onVoteCounts(counts)
                        commands = commands.map {
                            if (it.id == command.id) {
                                it.copy(options = it.options.map { option ->
                                    option.copy(count = counts[option.value] ?: option.count)
                                })
                            } else it
                        }
                    }
                }
                if (command.kind == DanmakuCommandKind.Grade) {
                    val latest = controller.commands(aid, cid)
                        .firstOrNull { it.interactionId == command.interactionId && it.kind == command.kind }
                    if (latest != null) {
                        commands = commands.map { if (it.id == command.id) latest.copy(id = command.id) else it }
                    }
                }
            } finally {
                pending = null
            }
        }
    }
}

/** 一帧卡片位置/尺寸；X 还需按实测卡片高度再夹一次，Y 由 Host 处理。 */
internal data class CommandCardPlacement(
    val width: Dp,
    val maxHeight: Dp,
    val x: Float,
    val y: Float,
    val minX: Float,
    val maxX: Float,
    val topLimit: Float,
    val bottomLimit: Float,
)

/**
 * 按真实视频画面（fit 后有黑边）计算卡片限制，而不是按播放器 View。
 * 没有解码尺寸时退回整个容器，和旧行为一致。
 */
internal fun commandCardPlacement(
    command: DanmakuCommand,
    containerWidthPx: Float,
    containerHeightPx: Float,
    density: Float,
    videoWidth: Int,
    videoHeight: Int,
): CommandCardPlacement {
    val videoAspect = if (videoWidth > 0 && videoHeight > 0) {
        videoWidth.toFloat() / videoHeight
    } else null
    val containerAspect = if (containerHeightPx > 0f) containerWidthPx / containerHeightPx else 0f
    val contentLeft: Float
    val contentTop: Float
    val contentWidth: Float
    val contentHeight: Float
    if (videoAspect != null && videoAspect > 0f && containerAspect > 0f) {
        if (containerAspect > videoAspect) {
            contentHeight = containerHeightPx
            contentWidth = (containerHeightPx * videoAspect).coerceAtMost(containerWidthPx)
            contentLeft = (containerWidthPx - contentWidth) / 2f
            contentTop = 0f
        } else {
            contentWidth = containerWidthPx
            contentHeight = (containerWidthPx / videoAspect).coerceAtMost(containerHeightPx)
            contentLeft = 0f
            contentTop = (containerHeightPx - contentHeight) / 2f
        }
    } else {
        contentLeft = 0f
        contentTop = 0f
        contentWidth = containerWidthPx
        contentHeight = containerHeightPx
    }
    val safeInsetPx = 8.dp.value * density
    // 关注/投票这类一行多个控件的卡片，半屏宽度会把按钮挤扁；给它们一个可读的最小宽度。
    val minCardWidthPx = when (command.kind) {
        DanmakuCommandKind.Follow -> 216.dp.value * density
        DanmakuCommandKind.Vote -> 200.dp.value * density
        DanmakuCommandKind.Grade -> 180.dp.value * density
        else -> 120.dp.value * density
    }
    val widthPx = (contentWidth * 0.5f).coerceAtLeast(minCardWidthPx)
        .coerceAtMost(320.dp.value * density)
        .coerceAtMost((contentWidth - safeInsetPx * 2f).coerceAtLeast(1f))
        .coerceAtLeast(1f)
    val maxWidthPx = contentLeft + contentWidth - widthPx - safeInsetPx
    val rawX = contentLeft + contentWidth * (command.x ?: 0.5f) - widthPx / 2f
    val rawY = command.y?.let { contentTop + contentHeight * it }
    val bottomLimit = contentTop + contentHeight - safeInsetPx
    return CommandCardPlacement(
        width = (widthPx / density).dp,
        maxHeight = ((contentHeight * 0.5f).coerceAtLeast(1f) / density).dp,
        x = rawX.coerceIn(contentLeft + safeInsetPx, maxWidthPx.coerceAtLeast(contentLeft + safeInsetPx)),
        y = if (rawY != null) rawY.coerceIn(contentTop + safeInsetPx, bottomLimit) else
            contentTop + contentHeight - 52.dp.value * density,
        minX = contentLeft + safeInsetPx,
        maxX = maxWidthPx.coerceAtLeast(contentLeft + safeInsetPx),
        topLimit = contentTop + safeInsetPx,
        bottomLimit = bottomLimit,
    )
}

/** 卡片主体：负责测量高度后夹位置，以及投票/评分成功后的局部状态刷新。 */
@Composable
internal fun DanmakuCommandHost(
    command: DanmakuCommand,
    session: DanmakuCommandSession,
    videoWidth: Int,
    videoHeight: Int,
    actions: VideoInteractionActions,
    videoLiked: Boolean,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val placement = commandCardPlacement(
            command,
            with(density) { maxWidth.toPx() },
            with(density) { maxHeight.toPx() },
            density.density,
            videoWidth,
            videoHeight,
        )
        var cardHeightPx by remember(command.id) { mutableIntStateOf(0) }
        var voteCounts by remember(command.id) { mutableStateOf(emptyMap<Int, Long>()) }
        var showVoteRatio by remember(command.id) { mutableStateOf(command.selected != null) }
        LaunchedEffect(command.id, command.interactionId, command.selected) {
            if (command.kind == DanmakuCommandKind.Vote && command.selected != null &&
                command.interactionId > 0 && voteCounts.isEmpty()
            ) {
                val counts = session.voteCounts(command.interactionId)
                if (counts.isNotEmpty()) voteCounts = counts
            }
        }
        var offsetX = placement.x
        var offsetY = placement.y
        if (cardHeightPx > 0) {
            offsetY = offsetY.coerceAtMost((placement.bottomLimit - cardHeightPx).coerceAtLeast(placement.topLimit))
            offsetX = offsetX.coerceAtMost(placement.maxX)
        }
        DanmakuCommandCard(
            command, session.selections[command.id] ?: command.selected, session.pending != null,
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .width(placement.width)
                .heightIn(max = placement.maxHeight)
                .alpha(if (cardHeightPx == 0) 0f else 1f)
                .onSizeChanged { cardHeightPx = it.height },
            actions, videoLiked, voteCounts, showVoteRatio,
            onLink = { session.openLink(command) },
            onDismiss = { session.dismiss(command.id) },
        ) { value ->
            session.submit(command, value, { showVoteRatio = true }, { voteCounts = it })
        }
    }
}

/** 关联视频确认弹窗。 */
@Composable
internal fun CommandLinkDialog(
    target: DanmakuCommand,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
) {
    WindowDialog(show = true, title = "打开关联视频", summary = target.text, onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiuixButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("取消")
            }
            MiuixButton(
                onClick = { onOpen(target.linkBvid) },
                modifier = Modifier.weight(1f),
            ) { Text("打开") }
        }
    }
}
