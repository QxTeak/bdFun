package com.aritxonly.bdfun

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.ui.AccountVipBadge
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SpaceProfileBadges(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SpaceProfileLevelVip(content, actions)
        SpaceProfileMedal(content, actions)
    }
}

@Composable
internal fun SpaceProfileLevelVip(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val profile = content.snapshot.profile.space?.profile
    Image(
        painter = painterResource(
            id = when ((profile?.level ?: 0).coerceIn(1, 6)) {
                1 -> R.drawable.bili_lv1
                2 -> R.drawable.bili_lv2
                3 -> R.drawable.bili_lv3
                4 -> R.drawable.bili_lv4
                5 -> R.drawable.bili_lv5
                else -> R.drawable.bili_lv6
            },
        ),
        contentDescription = "LV${profile?.level ?: 0}",
        modifier = Modifier.size(width = 26.dp, height = 12.dp),
    )
    // 大会员等级胶囊与「我的」页共用 AccountVipBadge：有大会员用品牌粉底白字，
    // 没有大会员也显示灰色的「大会员」；年度/十年/百年等文字来自接口 vip.label.text。
    Spacer(Modifier.width(5.dp))
    val isVip = profile?.vip == true
    AccountVipBadge(
        text = if (isVip) profile?.vipLabel.orEmpty().ifBlank { "大会员" } else "大会员",
        active = isVip,
    )
}

@Composable
internal fun SpaceProfileMedal(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val colors = MiuixTheme.colorScheme
    val profile = content.snapshot.profile.space?.profile
    if (!profile?.fansMedalName.isNullOrBlank()) {
        Spacer(Modifier.width(5.dp))
        val medalRgb = profile?.fansMedalColor ?: 0
        val medalColor = if (medalRgb != 0) {
            Color(0xFF000000L or (medalRgb.toLong() and 0xFFFFFFL))
        } else {
            colors.primary
        }
        // 和视频卡片里的 UP 小胶囊同一套样式：
        // 半透明底色 + 同色文字，四周只留刚好贴住文字的留白。
        Box(
            modifier = Modifier
                .clip(com.kyant.shapes.Capsule())
                .background(medalColor.copy(alpha = 0.16f))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text(
                text = "${profile?.fansMedalName} Lv.${profile?.fansMedalLevel}",
                color = medalColor,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
