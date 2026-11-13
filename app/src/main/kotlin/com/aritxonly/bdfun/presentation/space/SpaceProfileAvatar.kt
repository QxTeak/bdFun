package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.rememberImageRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SpaceProfileAvatar(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val colors = MiuixTheme.colorScheme
    val profile = content.snapshot.profile.space?.profile
    Box(modifier = Modifier.offset(y = (-38).dp).size(96.dp)) {
        AsyncImage(
            model = rememberImageRequest(profile?.face.orEmpty()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(5.dp, colors.background, CircleShape),
        )
        // 头像右侧的浅色弧线 + 小圆点组合成点缀，
        // 箭头不画完：弧线穿过小圆中心后再多留出一小段。
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(94.dp),
        ) {
            val stroke = 3.dp.toPx()
            val arcColor = colors.primary.copy(alpha = 0.45f)
            // 小圆在约 35° 处，两边各留约 10° 的空隙，
            // 弧线被小圆断成两段，两段都用圆角收尾。
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 115f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
            drawArc(
                color = arcColor,
                startAngle = 45f,
                sweepAngle = 15f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
        }
        // 头像旁不再显示大会员角标，大会员只在 LV 旁边显示。
        // 点缀：浅粉色弧线，小粉色圆点正好压在这条弧线的中段。
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 38.5.dp, y = 27.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.45f)),
        )
    }
}

@Composable
internal fun SpaceAvatarStats(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val colors = MiuixTheme.colorScheme
    val mid = content.mid
    val profile = content.snapshot.profile.space?.profile
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 头像放大，压在头图上；右侧加一颗主题色小圆点做点缀。
        SpaceProfileAvatar(content, actions)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .offset(y = (-4).dp),
        ) {
            // 粉丝 / 关注 / 获赞：和其他圆角卡片同款白色底。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceContainer)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpaceStat("粉丝", profile?.follower ?: 0, Modifier.weight(1f).clickable { RelationActivity.open(context, mid, true) })
                SpaceStat("关注", profile?.following ?: 0, Modifier.weight(1f).clickable { RelationActivity.open(context, mid, false) })
                SpaceStat("获赞", profile?.likes ?: 0, Modifier.weight(1f))
            }

        }
    }
}
