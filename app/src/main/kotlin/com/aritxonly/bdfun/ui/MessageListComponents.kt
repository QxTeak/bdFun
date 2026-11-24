package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aritxonly.bdfun.data.MessageItem
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageResumeEffect(onResume: suspend () -> Unit) {
    MessageResumeEffect(rememberCoroutineScope(), onResume)
}

@Composable
internal fun MessageResumeEffect(scope: kotlinx.coroutines.CoroutineScope, onResume: suspend () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val current by rememberUpdatedState(onResume)
    DisposableEffect(owner, scope) {
        var first = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (first) first = false else scope.launch { current() }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
internal fun MessageRowSurface(first: Boolean, last: Boolean, content: @Composable () -> Unit) {
    if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
        // 经典：整行平铺，不再画行分割线。
        Box(Modifier.fillMaxWidth().background(MiuixTheme.colorScheme.surfaceContainer)) { content() }
        return
    }
    val shape = remember(first, last) { MessageGroupShape(first, last) }
    Box(Modifier.fillMaxWidth().clip(shape).background(MiuixTheme.colorScheme.surfaceContainer)) { content() }
}

@Composable
internal fun MessagePageFooter(loading: Boolean, error: String?, hasMore: Boolean, empty: Boolean, onLoad: () -> Unit) {
    // This is a real LazyColumn item, so entering the viewport starts the next page.
    // Keep failed requests manual to avoid a retry loop while offline.
    LaunchedEffect(loading, hasMore, error) { if (!loading && hasMore && error == null) onLoad() }
    if (loading) {
        // 和其它页面一致：加载态用 Miuix 的环形+圆点动画，而不是一行「加载中…」。
        MiuixLoadingRow(modifier = Modifier.fillMaxWidth(), verticalPadding = 24.dp)
        return
    }
    Text(when { error != null -> "$error · 点击重试";
        hasMore -> "加载更多"; empty -> "暂无消息"; else -> "没有更多了" },
        color = MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 13.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable(enabled = error != null || hasMore, onClick = onLoad)
            .padding(vertical = 24.dp))
}

@Composable
internal fun SystemNotificationRow(item: MessageItem, onMore: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val currentOnMore by rememberUpdatedState(onMore)
    Column(Modifier.fillMaxWidth()
        .pointerInput(Unit) { detectTapGestures(onLongPress = { currentOnMore() }) }
        .semantics { onLongClick(label = "消息操作") { onMore(); true } }
        .padding(16.dp)) {
        Text(item.title, color = colors.onSurfaceContainer, fontSize = 16.sp)
        // Notification bodies contain HTML links. Render text and links, never a WebView or script.
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { context -> android.widget.TextView(context).apply {
                textSize = 14f
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
            } },
            update = { view ->
                val html = android.text.SpannableStringBuilder(android.text.Html.fromHtml(item.content,
                    android.text.Html.FROM_HTML_MODE_LEGACY))
                html.getSpans(0, html.length, android.text.style.URLSpan::class.java).forEach {
                    val scheme = android.net.Uri.parse(it.url).scheme
                    if (scheme != "http" && scheme != "https") html.removeSpan(it)
                }
                view.setOnLongClickListener { onMore(); true }
                view.text = html
                view.setTextColor(colors.onSurfaceContainer.toArgb())
                view.setLinkTextColor(colors.primary.toArgb())
            }, modifier = Modifier.fillMaxWidth())
        Text(item.source, color = colors.onBackgroundVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

private class MessageGroupShape(val first: Boolean, val last: Boolean) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val extra = with(density) { 44.dp.toPx() }
        val top = if (first) 0f else extra
        val bottom = if (last) 0f else extra
        val outline = com.aritxonly.teak.common.chrome.TeakContinuousCornerShape(22.dp)
            .createOutline(size.copy(height = size.height + top + bottom), layoutDirection, density)
        if (outline !is androidx.compose.ui.graphics.Outline.Generic) return outline
        val path = androidx.compose.ui.graphics.Path().apply {
            addPath(outline.path, androidx.compose.ui.geometry.Offset(0f, -top))
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
