package com.aritxonly.bdfun.presentation.pgc

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.aritxonly.bdfun.ui.rememberImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/** 顶部头图的一张：横版封面 + 标题 + 一句推荐语。 */
internal data class BangumiBannerItem(
    val image: String,
    val title: String,
    val subtitle: String,
    val seasonId: Long,
)

/** 在已确认的扇形比例上整体放大 10%：卡宽、间距、高度、圆角、字号一起放大。 */
private const val FAN_SCALE = 1.1f

/** 单张卡占舞台宽度的比例；中间那张要撑住画面主体。 */
private const val CARD_WIDTH_RATIO = 0.72f * FAN_SCALE

/** 相邻槽位的水平间距，相对卡片宽度；两侧各露出一小条且整排不超出屏幕。 */
private const val SPACING_TO_CARD = 0.15f

/** 宽屏上限，避免平板上头图高得离谱。 */
private val MaxCardWidth = 360.dp * FAN_SCALE

/** 卡片高度之外的舞台余量，给透视倾斜留一点呼吸空间。 */
private val StageExtraHeight = 40.dp * FAN_SCALE

/** 相机距离（越大透视越轻）；默认值 8 对这么大的卡片太近，近边会明显放大、底边下坠。 */
private const val CARD_CAMERA_DISTANCE = 36f * FAN_SCALE

/** 自动切换间隔：留出动画时间后整体约 3.8 秒一张，与改造前一致。 */
private const val CYCLE_MS = 3_200L

/** 手动点卡/划动后暂停自动切换的时长；期间再操作会重新计时。 */
private const val MANUAL_PAUSE_MS = 8_000L
private const val EXIT_TRAVEL_MS = 420
private const val PER_SLOT_MS = 180

private val CardShape = RoundedCornerShape(13.dp)
private val FanEasing = CubicBezierEasing(0.22f, 0.9f, 0.24f, 1f)

/**
 * 番剧页顶部的扇形卡片：中间一张摆正，左右各两张像折屏一样绕竖轴向内收，
 * 最后一张停在右侧隐藏位等待入场。
 *
 * 切换时所有卡片沿同一方向移动一个槽位：出场的卡继续向内旋转并淡出，
 * 右（左）侧那张则转回 0°、放大、升到最前，落到中间。点侧边卡可以主动摆正，
 * 点中间卡打开对应番剧。
 */
@Composable
internal fun BangumiBanner(
    items: List<BangumiBannerItem>,
    onOpen: (Long) -> Unit,
) {
    val fan = remember(items) { items.distinctBy { it.seasonId }.take(BANGUMI_FAN_SLOT_COUNT) }
    if (fan.isEmpty()) {
        BangumiBannerPlaceholder()
        return
    }

    val count = fan.size
    // 只有刚好 6 张时左右两端才各有一张隐藏卡，能无缝环绕；不够时退化成静态扇形。
    val canCycle = count == BANGUMI_FAN_SLOT_COUNT
    val focus = remember(fan) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // 手动操作后暂停自动轮播；每次点卡/划动都重新计时 8 秒。
    var pausedUntil by remember(fan) { mutableLongStateOf(0L) }

    LaunchedEffect(fan, canCycle) {
        if (!canCycle) return@LaunchedEffect
        while (isActive) {
            val remaining = pausedUntil - SystemClock.elapsedRealtime()
            delay(maxOf(CYCLE_MS, remaining))
            if (SystemClock.elapsedRealtime() < pausedUntil) continue
            focus.animateTo(focus.targetValue + 1f, fanTween(1f))
        }
    }

    fun pauseAuto() {
        pausedUntil = SystemClock.elapsedRealtime() + MANUAL_PAUSE_MS
    }

    BangumiFanStage(
        fan = fan,
        canCycle = canCycle,
        focus = focus.value,
        onSelect = { index ->
            val item = fan[index]
            val target = focus.targetValue
            val targetSlot = bangumiFanSlot(index, target, count)
            when {
                !canCycle -> onOpen(item.seasonId)
                abs(targetSlot) < 0.5f -> onOpen(item.seasonId)
                else -> {
                    pauseAuto()
                    scope.launch {
                        focus.animateTo(target + targetSlot, fanTween(targetSlot))
                    }
                }
            }
        },
        onSwipe = { direction ->
            if (canCycle) {
                pauseAuto()
                scope.launch { focus.animateTo(focus.targetValue + direction, fanTween(1f)) }
            }
        },
    )
}

@Composable
private fun BangumiFanStage(
    fan: List<BangumiBannerItem>,
    canCycle: Boolean,
    focus: Float,
    onSelect: (Int) -> Unit,
    onSwipe: (Int) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth * CARD_WIDTH_RATIO).coerceAtMost(MaxCardWidth)
        val cardHeight = cardWidth * 9f / 16f
        val stageHeight = cardHeight + StageExtraHeight
        val density = LocalDensity.current
        val spacingPx = with(density) { (cardWidth * SPACING_TO_CARD).toPx() }
        val swipeThresholdPx = with(density) { 42.dp.toPx() }

        Box(
            Modifier
                .fillMaxWidth()
                .height(stageHeight)
                // 左/右划切换整组卡片；用 Initial pass 观察并在横向意图明确时消费，
                // 否则外层主 tab 的 HorizontalPager 会把这次滑动当成切换「首页/动态」。
                .pointerInput(canCycle, onSwipe) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var totalX = 0f
                        var totalY = 0f
                        var claimed = false
                        var switched = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            totalX += change.positionChange().x
                            totalY += change.positionChange().y
                            if (!claimed && abs(totalX) > viewConfiguration.touchSlop &&
                                abs(totalX) > abs(totalY)
                            ) {
                                claimed = true
                            }
                            if (claimed) {
                                change.consume()
                                if (!switched && abs(totalX) >= swipeThresholdPx) {
                                    onSwipe(if (totalX < 0f) 1 else -1)
                                    switched = true
                                }
                            }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            fan.forEachIndexed { index, item ->
                val slot = if (canCycle) {
                    bangumiFanSlot(index, focus, fan.size)
                } else {
                    index - (fan.size - 1) / 2f
                }
                key(item.seasonId) {
                    BangumiFanCard(
                        item = item,
                        slot = slot,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        spacingPx = spacingPx,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BangumiFanCard(
    item: BangumiBannerItem,
    slot: Float,
    cardWidth: Dp,
    cardHeight: Dp,
    spacingPx: Float,
    onClick: () -> Unit,
) {
    val alpha = bangumiFanAlpha(slot)
    val scale = bangumiFanScale(slot)
    val distance = abs(slot)
    // 中间的卡只加文字用的底部渐变；两侧压一层暗色把注意力让给中间。
    val sideScrim = if (distance < 0.5f) 0f else (0.18f + distance * 0.06f).coerceAtMost(0.32f)
    val labelAlpha = (1f - distance * 1.7f).coerceIn(0f, 1f)

    Box(
        Modifier
            .width(cardWidth)
            .height(cardHeight)
            .graphicsLayer {
                translationX = slot * spacingPx
                // 绕竖轴向内收；相机拉远一点，近边不会被透视放大到把底边压弯。
                rotationY = bangumiFanRotation(slot)
                cameraDistance = CARD_CAMERA_DISTANCE
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .zIndex(10f - distance)
            .shadow(9.dp, CardShape, clip = false)
            .clip(CardShape)
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .clickable(enabled = alpha > 0.05f, onClick = onClick),
    ) {
        AsyncImage(
            model = rememberImageRequest(item.image),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (sideScrim > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = sideScrim)))
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = labelAlpha }
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))),
        )
        BangumiFanCardLabels(item, labelAlpha, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun BangumiFanCardLabels(
    item: BangumiBannerItem,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0.01f) return
    Column(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .padding(start = 13.dp, end = 13.dp, bottom = 11.dp),
    ) {
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun BangumiBannerPlaceholder() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(218.dp)
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
    )
}

/** 位移越大动画越长，点两张之外的卡时不会显得急停。 */
private fun fanTween(distance: Float): TweenSpec<Float> =
    tween(
        durationMillis = EXIT_TRAVEL_MS + (abs(distance) * PER_SLOT_MS).roundToInt(),
        easing = FanEasing,
    )
