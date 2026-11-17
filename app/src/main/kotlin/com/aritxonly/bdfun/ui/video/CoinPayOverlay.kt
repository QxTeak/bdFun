package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aritxonly.bdfun.R
import kotlinx.coroutines.delay

/**
 * 投币页：整页浮层，布局与动效照搬 PiliPlus `lib/pages/video/pay_coins/view.dart`。
 *
 * - 两枚硬币箱（选中 100dp / 未选中 70dp）带左右切换箭头；
 * - 角色图（1 枚 / 2 枚 / 余额不足）110×155，点一下或向上滑都投币；
 * - 硬币余额、已投提示、同时点赞复选框、关闭按钮；
 * - 投币时角色起手 → 硬币上抛淡出 → 提交，约 650ms，与上游的 50ms/300ms 两段节奏一致。
 *
 * 资源直接取自 PiliPlus `assets/images/paycoins/`（GPL-3.0，与 bdFun 一致）。
 */
@Composable
internal fun CoinPayOverlay(
    visible: Boolean,
    balance: Double?,
    alreadyCoined: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (count: Int, alsoLike: Boolean) -> Unit,
) {
    if (!visible) return
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.setDimAmount(0f)
            window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
            )
        }
        CoinPayContent(balance, alreadyCoined, onDismiss, onConfirm)
    }
}

private fun canPayCoins(index: Int, balance: Double?, alreadyCoined: Boolean): Boolean {
    if (index == 1 && alreadyCoined) return false
    if (balance == null) return true
    return balance >= (index + 1)
}

@Composable
private fun CoinPayContent(
    balance: Double?,
    alreadyCoined: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (count: Int, alsoLike: Boolean) -> Unit,
) {
    var selected by remember { mutableIntStateOf(0) }
    var alsoLike by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val canPay = !paying && canPayCoins(selected, balance, alreadyCoined)

    // 提交前先播完整段动作，视觉上"硬币真的投出去了"再关页。
    LaunchedEffect(paying) {
        if (!paying) return@LaunchedEffect
        val start = System.nanoTime()
        while (true) {
            val t = ((System.nanoTime() - start) / 1_000_000f / 650f).coerceIn(0f, 1f)
            progress = t
            if (t >= 1f) break
            delay(8)
        }
        onConfirm(selected + 1, alsoLike)
        paying = false
        onDismiss()
    }

    val pay = {
        if (canPay) {
            progress = 0f
            paying = true
        }
    }
    val switchTo = { target: Int ->
        if (!paying) selected = target.coerceIn(0, 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2000000))
            .pointerInput(paying) {
                if (paying) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        if (amount < 0 && selected == 0) switchTo(1)
                        if (amount > 0 && selected == 1) switchTo(0)
                    },
                )
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp + navBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 硬币箱：选中放大，两侧箭头切换；箭头在禁用态用灰版图片。
            Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoinSlot(index = 0, selected = selected == 0, progress = progress, paying = paying)
                    CoinSlot(index = 1, selected = selected == 1, progress = progress, paying = paying)
                }
                CoinArrow(
                    res = if (selected == 1) R.drawable.teak_coin_left else R.drawable.teak_coin_left_disable,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                    onClick = { if (selected == 1) switchTo(0) },
                )
                CoinArrow(
                    res = if (selected == 0) R.drawable.teak_coin_right else R.drawable.teak_coin_right_disable,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                    onClick = { if (selected == 0) switchTo(1) },
                )
            }
            Spacer(Modifier.height(4.dp))
            // 角色图：点一下或向上滑都能投币；不可投时换成"余额不足"图。
            val character = when {
                !canPayCoins(selected, balance, alreadyCoined) -> R.drawable.teak_coin_not_enough
                selected == 0 -> R.drawable.teak_coin_mario
                else -> R.drawable.teak_coin_gun_sister
            }
            Image(
                painter = painterResource(character),
                contentDescription = if (canPay) "点击或上滑投币" else "当前数量不可投币",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(110.dp, 155.dp)
                    .graphicsLayer {
                        // 起手：前 15% 轻轻上浮并放大，和上游 50ms 的按压缩放一致。
                        val press = (progress / 0.15f).coerceIn(0f, 1f)
                        val lift = if (press > 0f) (1f - (press * 2f - 1f).coerceIn(0f, 1f)) * 6.dp.toPx() else 0f
                        translationY = -lift
                        val scale = 1f + 0.08f * if (press > 0f) (1f - (press * 2f - 1f).coerceIn(0f, 1f)) else 0f
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerInput(paying, canPay) {
                        if (!canPay) return@pointerInput
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -20f) pay()
                            },
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { pay() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    balance == null -> "硬币余额：加载中…"
                    balance < 0 -> "硬币余额：暂不可用"
                    else -> "硬币余额：${formatCoinBalance(balance)}"
                },
                color = Color.White,
                fontSize = 13.sp,
            )
            if (alreadyCoined) {
                Text(text = "已投 1 枚硬币", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = alsoLike,
                        onCheckedChange = { if (!paying) alsoLike = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFFB7299),
                            uncheckedColor = Color.White.copy(alpha = 0.7f),
                            checkmarkColor = Color.White,
                        ),
                    )
                    Text(
                        text = "同时点赞",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { if (!paying) alsoLike = !alsoLike },
                    )
                }
                Image(
                    painter = painterResource(R.drawable.teak_coin_close),
                    contentDescription = "关闭",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onDismiss() },
                )
            }
        }
    }
}

@Composable
private fun CoinSlot(index: Int, selected: Boolean, progress: Float, paying: Boolean) {
    // 选中：箱 100dp / 币 50dp；未选中 70dp / 35dp（上游 factor 0..1 的线性插值）。
    val boxSize = if (selected) 100.dp else 70.dp
    val coinSize = if (selected) 50.dp else 35.dp
    Box(modifier = Modifier.size(boxSize), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.teak_coin_box),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        // 两页都画硬币，只是未选中的小一号（上游 factor 0..1 的尺寸插值）。
        Image(
            painter = painterResource(if (index == 0) R.drawable.teak_coins_one else R.drawable.teak_coins_two),
            contentDescription = null,
            modifier = Modifier
                .size(coinSize)
                .graphicsLayer {
                    if (!paying || !selected) return@graphicsLayer
                    // 硬币上抛 2 倍高度，最后 1/3 淡出。
                    val throwProgress = ((progress - 0.15f) / 0.5f).coerceIn(0f, 1f)
                    translationY = -throwProgress * 2f * size.height
                    alpha = (1f - ((progress - 0.65f) / 0.35f).coerceIn(0f, 1f))
                },
        )
    }
}

@Composable
private fun CoinArrow(
    res: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = modifier
            .size(16.dp, 28.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
    )
}

private fun formatCoinBalance(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.ROOT, "%.1f", value)
