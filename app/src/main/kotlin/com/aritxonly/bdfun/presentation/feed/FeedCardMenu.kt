package com.aritxonly.bdfun.ui

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.domain.DynamicReportReason
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.feature.feed.presentation.FeedState
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.window.WindowDialog

/** 危险操作统一用和首页「拉黑 UP」相同的红。 */
private val FeedDangerRed = Color(0xFFE5484D)

/** 和首页形变菜单一致：收起动画约 240ms，动作等回落再执行。 */
private const val FeedMenuDismissDelayMs = 240L

/**
 * 动态卡片省略号 / 长按共用的玻璃形变菜单。
 *
 * 宿主挂在主壳玻璃采样树之外（MainFeedChrome），面板从图标或手指位置长出来；
 * 条目和首页视频卡片不是一套业务，但外壳、动画、危险色完全复用同一套实现。
 */
@Composable
internal fun FeedCardMenuHost(state: FeedState, onMessage: (String) -> Unit) {
    var activeState by remember { mutableStateOf<TeakMorphMenuState?>(null) }
    var lastItem by remember { mutableStateOf<FeedItem?>(null) }
    LaunchedEffect(state.menuState, state.menuItem) {
        if (state.menuState != null) activeState = state.menuState
        if (state.menuItem != null) lastItem = state.menuItem
    }
    val menuState = activeState ?: return
    val item = state.menuItem ?: lastItem
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clearMenu: () -> Unit = {
        state.menuItem = null
        state.menuState = null
    }
    // 先收菜单再执行动作，和首页一致；动作执行失败也一定卸载宿主。
    val closeThen: (suspend () -> Unit) -> Unit = { after ->
        menuState.close()
        scope.launch {
            delay(FeedMenuDismissDelayMs)
            try {
                after()
            } finally {
                clearMenu()
            }
        }
    }
    val autoplayOn = AppGraph.settings.dynamicAutoPlay
    val selfMid = AppGraph.session.value?.mid
    TeakMorphMenuHost(
        state = menuState,
        onDismiss = clearMenu,
        minWidth = 200.dp,
        maxWidth = 280.dp,
        maxHeight = 420.dp,
    ) { _ ->
        FeedCardMenuRows(
            item = item,
            autoplayOn = autoplayOn,
            canUnfollow = item != null && item.mid > 0L && item.mid != selfMid,
            state = state,
            context = context,
            onMessage = onMessage,
            closeThen = closeThen,
        )
    }
}

/** 菜单行：内容和首页视频卡片不同，外壳/动画/危险色保持一致。 */
private class FeedMenuRow(
    val label: String,
    val enabled: Boolean,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun FeedCardMenuRows(
    item: FeedItem?,
    autoplayOn: Boolean,
    canUnfollow: Boolean,
    state: FeedState,
    context: android.content.Context,
    onMessage: (String) -> Unit,
    closeThen: (suspend () -> Unit) -> Unit,
) {
    val rows = listOf(
        watchLaterRow(item, closeThen, onMessage),
        copyRow(item, context, closeThen, onMessage),
        autoplayRow(autoplayOn, closeThen, onMessage),
        shareRow(item, context, closeThen),
        unfollowRow(item, canUnfollow, closeThen, onMessage),
        reportRow(item, state, closeThen),
    )
    rows.forEach { row ->
        GlassPopupItem(
            text = row.label,
            enabled = row.enabled,
            contentColor = if (row.danger) FeedDangerRed else Color.Unspecified,
            onClick = row.onClick,
        )
    }
}

private fun watchLaterRow(
    item: FeedItem?,
    closeThen: (suspend () -> Unit) -> Unit,
    onMessage: (String) -> Unit,
) = FeedMenuRow(
    label = "添加至稍后再看",
    enabled = item?.video?.aid?.let { it > 0L } == true,
) {
    closeThen {
        val aid = item?.video?.aid ?: return@closeThen
        when (val result = AppGraph.videoActions.watchLater(aid)) {
            is Outcome.Success -> onMessage("已添加至稍后再看")
            is Outcome.Failure -> onMessage(result.error.messageOr("添加失败"))
        }
    }
}

private fun copyRow(
    item: FeedItem?,
    context: android.content.Context,
    closeThen: (suspend () -> Unit) -> Unit,
    onMessage: (String) -> Unit,
) = FeedMenuRow(label = "复制", enabled = item != null) {
    closeThen {
        val target = item ?: return@closeThen
        val text = target.text.ifBlank { target.title }.ifBlank { feedDynamicLink(target) }
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("动态", text))
        onMessage("已复制")
    }
}

private fun autoplayRow(
    autoplayOn: Boolean,
    closeThen: (suspend () -> Unit) -> Unit,
    onMessage: (String) -> Unit,
) = FeedMenuRow(
    label = if (autoplayOn) "关闭自动播放" else "自动播放已关闭",
    enabled = autoplayOn,
) {
    closeThen {
        AppGraph.settings.dynamicAutoPlay = false
        onMessage("已关闭动态自动播放")
    }
}

private fun shareRow(
    item: FeedItem?,
    context: android.content.Context,
    closeThen: (suspend () -> Unit) -> Unit,
) = FeedMenuRow(label = "分享", enabled = item != null) {
    closeThen {
        val target = item ?: return@closeThen
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, feedDynamicLink(target))
                },
                "分享动态",
            ),
        )
    }
}

private fun unfollowRow(
    item: FeedItem?,
    canUnfollow: Boolean,
    closeThen: (suspend () -> Unit) -> Unit,
    onMessage: (String) -> Unit,
) = FeedMenuRow(label = "取消关注", enabled = canUnfollow, danger = true) {
    closeThen {
        val mid = item?.mid ?: return@closeThen
        when (val result = AppGraph.videoActions.follow(mid, false)) {
            is Outcome.Success -> onMessage("已取消关注")
            is Outcome.Failure -> onMessage(result.error.messageOr("取消关注失败"))
        }
    }
}

private fun reportRow(
    item: FeedItem?,
    state: FeedState,
    closeThen: (suspend () -> Unit) -> Unit,
) = FeedMenuRow(label = "举报", enabled = item != null, danger = true) {
    closeThen { state.reportItem = item }
}

/** 动态的网页地址：转发动态也用当前这条的 id，点开就是用户看到的那条。 */
private fun feedDynamicLink(item: FeedItem): String = "https://t.bilibili.com/${item.id}"

/**
 * 举报理由选择。理由编号对齐 PiliPlus；「其他」需要文字说明，这里暂不提供，
 * 免得提交一个服务端必拒的空说明。
 */
@Composable
internal fun DynamicReportDialog(
    item: FeedItem,
    onDismiss: () -> Unit,
    onSubmit: (DynamicReportReason) -> Unit,
) {
    var selected by remember(item.id) { mutableStateOf<DynamicReportReason?>(null) }
    var submitted by remember(item.id) { mutableStateOf(false) }
    WindowDialog(
        show = true,
        title = "举报动态",
        summary = item.text.take(48).ifBlank { item.name },
        onDismissRequest = { if (!submitted) onDismiss() },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DynamicReportReason.entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { option ->
                        Button(
                            onClick = { if (!submitted) selected = option },
                            enabled = !submitted,
                            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                            minHeight = 40.dp,
                            insideMargin = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = if (selected == option) "✓ ${option.title}" else option.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onDismiss,
                    enabled = !submitted,
                    modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                    minHeight = 42.dp,
                ) { Text("取消") }
                Button(
                    enabled = selected != null && !submitted,
                    modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                    minHeight = 42.dp,
                    onClick = {
                        val reason = selected ?: return@Button
                        submitted = true
                        onSubmit(reason)
                    },
                ) { Text("提交举报") }
            }
        }
    }
}
