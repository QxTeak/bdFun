package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.domain.fittingHotDanmakuIndices
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

data class HotDanmakuPresentation(val items: List<DanmakuItem> = emptyList(), val heightPx: Int = 0)

@Composable
internal fun BoxScope.PlayerHotDanmakuOverlay(
    ui: VideoPlayerUiStateHolder, fullscreen: Boolean, picture: VideoPictureSettings, danmakuVisible: Boolean,
) {
    val hit = ui.danmakuHit
    if (!picture.hotDanmakuEnabled || !danmakuVisible || ui.fsAudioOnly.value || ui.hotDanmaku.value.isEmpty()) return
    val votes by hit.votes.collectAsState()
    val textStyle = hotStyle(16f * picture.hotDanmakuTextScale)
    val countStyle = hotStyle(20f * picture.hotDanmakuTextScale).copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var pending by remember(hit.sendSame) { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    DisposableEffect(hit) { onDispose { hit.hotPresentation.value = HotDanmakuPresentation() } }
    BoxWithConstraints(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        val items = ui.hotDanmaku.value
        val widths = remember(items, votes, maxWidth, measurer, density, textStyle, countStyle) {
            with(density) {
                val measured = items.map { item ->
                    val count = votes[item.id]?.likes ?: item.likeCount
                    val countWidth = (measurer.measure("×$count", countStyle).size.width * 1.15f).toDp()
                    val textWidth = measurer.measure(item.text, textStyle).size.width
                    Triple(item, countWidth, textWidth + countWidth.toPx() + 50.dp.toPx())
                }
                fittingHotDanmakuIndices(measured.map { it.third }, maxWidth.toPx(), 4.dp.toPx()).map { measured[it] }
            }
        }
        val visible = remember(widths) { widths.map { it.first } }
        val ids = remember(visible) { visible.map { it.id } }
        var height by remember { mutableIntStateOf(0) }
        SideEffect { hit.hotPresentation.value = HotDanmakuPresentation(visible, if (visible.isEmpty()) 0 else height) }
        LaunchedEffect(ids, hit.loadVotes) { if (ids.isNotEmpty()) hit.loadVotes?.invoke(ids) }
        if (visible.isNotEmpty()) Row(
            Modifier.fillMaxWidth().onSizeChanged { height = it.height }.padding(top = if (fullscreen) 10.dp else 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
        ) {
            widths.forEach { (item, countWidth) ->
                key(item.id) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.text, color = Color.White, style = textStyle, maxLines = 1, softWrap = false)
                        AnimatedDanmakuCount(votes[item.id]?.likes ?: item.likeCount, countStyle, Modifier.width(countWidth))
                        val liked = votes[item.id]?.liked == true
                        HotAction(if (liked) "取消点赞" else "点赞弹幕", picture.danmakuLikeIcon(liked),
                            if (liked) picture.accentColor else Color.White) { hit.like?.invoke(item.id) }
                        HotAction("发一条同款弹幕", LucideIcons.SendHorizontalFilled, Color.White) { pending = item.text }
                    }
                }
            }
        }
    }
    pending?.let { message ->
        AlertDialog(onDismissRequest = { if (!sending) pending = null }, title = { Text("发一条同款弹幕") },
            text = { Text(message) }, confirmButton = {
                TextButton(enabled = !sending, onClick = {
                    val sender = hit.sendSame ?: return@TextButton
                    sending = true
                    scope.launch {
                        try { if (sender(message)) pending = null }
                        finally { sending = false }
                    }
                }) { Text("发送") }
            }, dismissButton = { TextButton(enabled = !sending, onClick = { pending = null }) { Text("取消") } })
    }
}

@Composable
internal fun AnimatedDanmakuCount(count: Long, style: TextStyle, modifier: Modifier = Modifier) {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(count) { target = count.toFloat() }
    val shown by animateFloatAsState(target, tween(320), label = "弹幕点赞计数")
    val integer = if (shown == target && target > 0f) count else shown.roundToLong()
    Text("×$integer", modifier, color = Color.White, style = style, maxLines = 1, softWrap = false)
}

private fun hotStyle(size: Float) = TextStyle(fontSize = size.sp, shadow = Shadow(Color.Black, Offset(1f, 1f), 3f))

@Composable
private fun HotAction(label: String, icon: Int, tint: Color, action: () -> Unit) {
    Box(Modifier.size(24.dp).semantics { contentDescription = label }.clickable(role = Role.Button, onClick = action),
        contentAlignment = Alignment.Center) {
        AppGlyph(icon, tint, size = 18.dp)
    }
}
