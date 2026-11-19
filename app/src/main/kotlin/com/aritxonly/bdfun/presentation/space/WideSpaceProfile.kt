package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigation.createRouteIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.ui.ContinuousCornerShape
import com.aritxonly.bdfun.ui.showToast
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 宽屏（横屏）个人空间左栏。
 *
 * 头像、LV/大会员、粉丝牌直接复用竖屏的 [SpaceProfileAvatar]、[SpaceProfileBadges]：
 * 三处标识的样式与竖屏保持同一份实现，粉丝牌也在等级同一行。
 */
@Composable
internal fun WideSpaceProfile(
    content: SpaceScreenContent,
    actions: SpaceScreenActions,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val profile = content.snapshot.profile.space?.profile
    val mid = content.mid
    val isSelf = content.isSelf
    val followed = content.snapshot.profile.followed
    val busy = content.snapshot.profile.followBusy
    Column(Modifier.fillMaxWidth().background(colors.background), horizontalAlignment = Alignment.CenterHorizontally) {
        SpaceBanner(content.banner.bitmap, aspect = 2.35f)
        // 头像描边、压图偏移和弧线点缀与竖屏同一份实现；资料列上移抵消头像只占位的 96dp。
        SpaceProfileAvatar(content, actions)
        Column(Modifier.fillMaxWidth().offset(y = (-38).dp).trimBottom(24.dp).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(profile?.name.orEmpty(), color = colors.primary, fontSize = 21.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Medium)
            SpaceProfileBadges(content, actions)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SpaceStat("粉丝", profile?.follower ?: 0, Modifier.weight(1f).clickable { RelationActivity.open(context, mid, true) })
                SpaceStat("关注", profile?.following ?: 0, Modifier.weight(1f).clickable { RelationActivity.open(context, mid, false) })
                SpaceStat("获赞", profile?.likes ?: 0, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FollowCapsule(isSelf = isSelf, followed = followed, busy = busy, modifier = Modifier.weight(1f), onClick = actions.onFollow)
                if (!isSelf) MessageCircleButton {
                    context.startActivity(createRouteIntent(context,
                        com.aritxonly.bdfun.core.navigation.Route.PrivateChat(mid, name = profile?.name.orEmpty(),
                            avatar = profile?.face.orEmpty())))
                }
            }
            Text(profile?.sign.orEmpty().ifBlank { "这个人很神秘，什么都没有写" }, color = colors.onBackgroundVariant,
                modifier = Modifier.fillMaxWidth().clip(ContinuousCornerShape(18.dp)).background(colors.surfaceContainer).padding(16.dp), fontSize = 13.sp)
            if (!profile?.officialTitle.isNullOrBlank()) Text(profile?.officialTitle.orEmpty(), color = colors.onBackgroundVariant, fontSize = 12.sp)
            if (!profile?.officialDesc.isNullOrBlank()) Text(profile?.officialDesc.orEmpty(), color = colors.onBackgroundVariant, fontSize = 12.sp)
            Text("UID：$mid", color = colors.onBackgroundVariant, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            Text("充电 · ${formatCountNoDecimal(profile?.chargingCount ?: 0)}人已为其充电", color = colors.onBackgroundVariant,
                fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clip(ContinuousCornerShape(18.dp))
                    .background(colors.surfaceContainer).clickable {
                        runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://space.bilibili.com/$mid/upower/rank"))) }
                            .onFailure { context.showToast("无法打开充电页") }
                    }.padding(16.dp))
        }
    }
}
