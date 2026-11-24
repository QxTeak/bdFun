package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.R
import com.aritxonly.teak.common.settings.TeakSettingsPage
import com.kyant.shapes.Capsule
import kotlin.math.abs
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val AlipayBlue = Color(0xFF1677FF)
private val WeChatGreen = Color(0xFF07C160)

@Composable
fun DonationScreen() {
    var method by remember { mutableIntStateOf(0) }

    TeakSettingsPage(title = "捐赠") {
        item(key = "methods") {
            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DonationMethodChip(
                        text = "微信",
                        selected = method == 0,
                        accent = WeChatGreen,
                        modifier = Modifier.weight(1f),
                    ) { method = 0 }
                    DonationMethodChip(
                        text = "支付宝",
                        selected = method == 1,
                        accent = AlipayBlue,
                        modifier = Modifier.weight(1f),
                    ) { method = 1 }
                }
            }
        }

        item(key = "qr") {
            DonationQrCard(method = method, onToggle = { method = 1 - method })
        }

        item(key = "thanks") {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "感谢您对开源社区的支持与贡献",
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 13.sp,
                )
            }
        }

        item(key = "space") { Spacer(Modifier.height(24.dp)) }
    }
}

/** 收款码区域：二维码不再套外层圆角矩形，按屏幕可用高度垂直居中。 */
@Composable
private fun DonationQrCard(method: Int, onToggle: () -> Unit) {
    var dragTotal by remember { mutableFloatStateOf(0f) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((screenHeight * 0.62f).coerceAtLeast(360.dp))
            .pointerInput(method) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        if (abs(dragTotal) > 70f) onToggle()
                        dragTotal = 0f
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragTotal += amount
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (method == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DonationQr(
                    image = R.drawable.donation_qr,
                    name = "绮锡Teak",
                    description = "绮锡Teak 微信收款码",
                    accent = WeChatGreen,
                    modifier = Modifier.weight(1f),
                )
                DonationQr(
                    image = R.drawable.donation_qr_partner,
                    name = "音唯AritxOnly",
                    description = "音唯AritxOnly 微信收款码",
                    accent = WeChatGreen,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            DonationQr(
                image = R.drawable.donation_alipay,
                name = "绮锡Teak",
                description = "绮锡Teak 支付宝收款码",
                accent = AlipayBlue,
                modifier = Modifier.fillMaxWidth(0.72f),
            )
        }
    }
}

/** 一张收款码 + 名字 + 对应颜色的「保存二维码」胶囊。 */
@Composable
private fun DonationQr(
    image: Int,
    name: String,
    description: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                // 两张收款码统一圆角，避免外层圆角矩形改完后一张有一张没有。
                .clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 12.sp,
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(30.dp)
                .clip(Capsule())
                .background(accent)
                .clickable(interactionSource = null, indication = null) {
                    scope.launch {
                        val ok = ImageSaver.saveDrawable(context, image, name + "_" + description)
                        context.showToast(if (ok) "二维码已保存到相册" else "保存失败")
                    }
                }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "保存二维码",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DonationMethodChip(
    text: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(Capsule())
            .background(if (selected) accent else colors.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else colors.onBackgroundVariant,
            fontSize = 14.sp,
        )
    }
}
