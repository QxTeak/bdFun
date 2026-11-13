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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import com.aritxonly.bdfun.trimBottom
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import androidx.compose.foundation.layout.PaddingValues
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
            .background(colors.surfaceContainer, cardShape)
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().trimBottom(24.dp), verticalAlignment = Alignment.Bottom) {
            AsyncImage(
                model = rememberImageRequest(shown.face), contentDescription = "头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(76.dp).clip(CircleShape).background(colors.surfaceContainerHigh)
                    .border(5.dp, colors.surfaceContainer, CircleShape),
            )
            Spacer(Modifier.weight(1f))
            Button(onClick = { navigation.onOpenSpace(shown.mid) }, cornerRadius = 16.dp, minWidth = 0.dp,
                colors = ButtonDefaults.buttonColors(color = colors.surfaceContainerHigh, contentColor = colors.onBackground),
                minHeight = 32.dp, insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Text("个人空间 ↗", fontSize = 11.sp, color = colors.onBackground)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { MineIdentity(shown) }

        Spacer(Modifier.height(22.dp))
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
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (shown.vip) {
                Text(shown.vipLabel.ifBlank { "大会员" }, color = com.aritxonly.bdfun.BrandPink,
                    fontSize = 11.sp, modifier = Modifier.clip(CircleShape)
                        .background(com.aritxonly.bdfun.BrandPink.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 3.dp))
                Spacer(Modifier.width(9.dp))
            }
            Text("硬币 ${"%.1f".format(shown.coins)}", color = colors.onBackgroundVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MineNameAndLevel(shown: UserProfile) {
    val colors = MiuixTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = shown.name,
            color = colors.onBackground,
            fontSize = 23.sp,
            lineHeight = 28.sp,
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
