package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.R
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsPage
import com.kyant.shapes.Capsule
import kotlin.math.abs
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val AlipayBlue = Color(0xFF1677FF)
private val WeChatGreen = Color(0xFF07C160)

@Composable
fun DonationScreen() {
    val colors = MiuixTheme.colorScheme
    var method by remember { mutableIntStateOf(0) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

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
            TeakSettingsCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .pointerInput(method) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onDragEnd = {
                                    if (abs(dragTotal) > 70f) method = 1 - method
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
                        // 微信：作者和合作伙伴各一张收款码，不加任何文字。
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            DonationQr(R.drawable.donation_qr, "微信捐赠二维码", colors)
                            DonationQr(R.drawable.donation_qr_partner, "合作伙伴微信收款码", colors)
                        }
                    } else {
                        Image(
                            painter = painterResource(R.drawable.donation_alipay),
                            contentDescription = "支付宝捐赠二维码",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surfaceContainer),
                        )
                    }
                }
            }
        }

        item(key = "space") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DonationQr(
    image: Int,
    description: String,
    colors: top.yukonga.miuix.kmp.theme.Colors,
) {
    Image(
        painter = painterResource(image),
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceContainer),
    )
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
            fontWeight = if (selected) FontWeight.Normal else FontWeight.Normal,
        )
    }
}
