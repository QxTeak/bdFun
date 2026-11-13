package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.core.model.user.UserProfile
import com.aritxonly.bdfun.feature.mine.application.*

@Composable
internal fun MineProfileCard(shown: UserProfile, cardShape: androidx.compose.ui.graphics.Shape, navigation: MineNavigation) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surfaceContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = rememberImageRequest(shown.face),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh),
            )
            Spacer(Modifier.width(12.dp))
            // 三行整体高度压在头像（58dp）以内：ID / 大会员 / 硬币，首字左对齐
            MineIdentity(shown)
            Text(
                text = "空间 ＞",
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
                modifier = Modifier.clickable(interactionSource = null, indication = null) {
                    navigation.onOpenSpace(shown.mid)
                },
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell("关注", shown.following, Modifier.weight(1f).clickable { navigation.onOpenRelation(shown.mid, false) })
            StatCell("粉丝", shown.follower, Modifier.weight(1f).clickable { navigation.onOpenRelation(shown.mid, true) })
            StatCell("获赞", shown.likes, Modifier.weight(1f))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.MineIdentity(shown: UserProfile) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = Modifier.weight(1f)) {
        MineNameAndLevel(shown)
        Spacer(Modifier.height(4.dp))
        if (shown.vip) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(com.aritxonly.bdfun.BrandPink)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    text = shown.vipLabel.ifBlank { "大会员" },
                    color = Color.White,
                    fontSize = 8.5.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                )
            }
        } else {
            Spacer(Modifier.height(14.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "硬币 ${"%.1f".format(shown.coins)}",
            color = colors.onBackgroundVariant,
            fontSize = 11.sp,
            lineHeight = 13.sp,
        )
    }
}

@Composable
private fun MineNameAndLevel(shown: UserProfile) {
    val colors = MiuixTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = shown.name,
            color = colors.onBackground,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(4.dp))
        Image(
            painter = painterResource(
                id = when (shown.level.coerceIn(1, 6)) {
                    1 -> com.aritxonly.bdfun.R.drawable.bili_lv1
                    2 -> com.aritxonly.bdfun.R.drawable.bili_lv2
                    3 -> com.aritxonly.bdfun.R.drawable.bili_lv3
                    4 -> com.aritxonly.bdfun.R.drawable.bili_lv4
                    5 -> com.aritxonly.bdfun.R.drawable.bili_lv5
                    else -> com.aritxonly.bdfun.R.drawable.bili_lv6
                },
            ),
            contentDescription = "LV${shown.level}",
            modifier = Modifier.size(width = 22.dp, height = 10.dp),
        )
    }
}
