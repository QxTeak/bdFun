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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.domain.DanmakuVote
import com.aritxonly.bdfun.core.domain.fittingHotDanmakuIndices
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

data class HotDanmakuPresentation(val heightPx: Int = 0, val reservedTopPx: Int = 0)

@Composable
internal fun BoxScope.PlayerHotDanmakuOverlay(
    ui: VideoPlayerUiStateHolder, fullscreen: Boolean, picture: VideoPictureSettings, danmakuVisible: Boolean,
) {
    val hit = ui.danmakuHit
    if (!picture.hotDanmakuEnabled || !danmakuVisible || ui.fsAudioOnly.value) return
    val votes by hit.votes.collectAsState()
    val textStyle = hotStyle(DANMAKU_TEXT_SIZE_SP * picture.hotDanmakuTextScale)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var pending by remember(hit.sendSame) { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    DisposableEffect(hit) { onDispose { hit.hotPresentation.value = HotDanmakuPresentation() } }
    BoxWithConstraints(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        val window by ui.hotDanmaku
        val topPadding = if (fullscreen) 10.dp else 4.dp
        val measurements = remember(window, votes, maxWidth, measurer, density, textStyle) {
            HotDanmakuMeasurement(measurer, density, textStyle, maxWidth)
                .measure(window.visible, window.upcoming, votes)
        }
        val widths = measurements.visible
        val ids = remember(widths) { widths.map { it.item.id } }
        var height by remember { mutableIntStateOf(0) }
        SideEffect {
            val visibleHeight = if (widths.isEmpty()) 0 else height
            val measuredReserve = measurements.reservedContentHeightPx.takeIf { it > 0 }?.let {
                it + with(density) { topPadding.roundToPx() }
            } ?: 0
            hit.hotPresentation.value = HotDanmakuPresentation(
                heightPx = visibleHeight,
                reservedTopPx = maxOf(visibleHeight, measuredReserve),
            )
        }
        LaunchedEffect(ids, hit.loadVotes) { if (ids.isNotEmpty()) hit.loadVotes?.invoke(ids) }
        if (widths.isNotEmpty()) Row(
            Modifier.fillMaxWidth().onSizeChanged { height = it.height }.padding(top = topPadding),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
        ) {
            widths.forEach { (item, countWidth, rowStyle) ->
                key(item.id) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.text, color = Color.White, style = rowStyle, maxLines = 1, softWrap = false)
                        AnimatedDanmakuCount(votes[item.id]?.likes ?: item.likeCount, rowStyle, Modifier.width(countWidth))
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

private data class HotDanmakuRowMeasurement(
    val item: DanmakuItem, val countWidth: Dp, val textStyle: TextStyle, val widthPx: Float, val heightPx: Int,
)

private data class HotDanmakuMeasurements(
    val visible: List<HotDanmakuRowMeasurement>, val reservedContentHeightPx: Int,
)

private class HotDanmakuMeasurement(
    private val measurer: TextMeasurer, private val density: Density,
    private val textStyle: TextStyle, private val maxWidth: Dp,
) {
    fun measure(
        visible: List<DanmakuItem>, upcoming: List<DanmakuItem>, votes: Map<Long, DanmakuVote>,
    ): HotDanmakuMeasurements = with(density) {
        val rows = visible.map { measureItem(it, votes[it.id]) }
        val indices = fittingHotDanmakuIndices(rows.map { it.widthPx }, maxWidth.toPx(), 4.dp.toPx())
        val usableWidth = maxWidth.toPx() - 2 * 4.dp.toPx()
        // 将要显示的条目也按完整宽度判断，放不下的长文不应空占轨道。
        val futureHeight = upcoming.maxOfOrNull { item ->
            val row = measureItem(item, votes[item.id])
            if (row.widthPx <= usableWidth) row.heightPx else 0
        } ?: 0
        HotDanmakuMeasurements(indices.map { rows[it] }, maxOf(futureHeight, indices.maxOfOrNull { rows[it].heightPx } ?: 0))
    }

    private fun measureItem(item: DanmakuItem, vote: DanmakuVote?): HotDanmakuRowMeasurement = with(density) {
        val rowStyle = textStyle.copy(fontSize = textStyle.fontSize * danmakuItemTextScale(item.size))
        val count = measurer.measure("×${vote?.likes ?: item.likeCount}", rowStyle).size
        val text = measurer.measure(item.text, rowStyle).size
        val countWidth = (count.width * 1.15f).toDp()
        HotDanmakuRowMeasurement(item, countWidth, rowStyle, text.width + countWidth.toPx() + 50.dp.toPx(),
            maxOf(text.height, count.height, 24.dp.roundToPx()))
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

const val DANMAKU_TEXT_SIZE_SP = 12f
fun danmakuItemTextScale(size: Float): Float = size.coerceIn(0.75f, 1.25f)

private fun hotStyle(size: Float) = TextStyle(
    fontSize = size.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
    shadow = Shadow(Color.Black, Offset(1f, 1f), 3f),
)

@Composable
private fun HotAction(label: String, icon: Int, tint: Color, action: () -> Unit) {
    Box(Modifier.size(24.dp).semantics { contentDescription = label }.clickable(role = Role.Button, onClick = action),
        contentAlignment = Alignment.Center) {
        AppGlyph(icon, tint, size = 18.dp)
    }
}
