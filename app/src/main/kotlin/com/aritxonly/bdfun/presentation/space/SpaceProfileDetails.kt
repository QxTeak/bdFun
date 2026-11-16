package com.aritxonly.bdfun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.navigation.createRouteIntent
import com.aritxonly.bdfun.ui.showToast
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SpaceProfileDetails(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val colors = MiuixTheme.colorScheme
    val mid = content.mid
    val profile = content.snapshot.profile.space?.profile
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .trimBottom(24.dp)
            .padding(start = 16.dp, end = 16.dp, top = 0.dp),
    ) {
        SpaceProfileNameActions(content, actions)
        Spacer(Modifier.height(8.dp))
        SpaceProfileBadges(content, actions)

        SpaceOfficialProfile(content, actions)

        Spacer(Modifier.height(10.dp))
        ExpandableSign(
            text = profile?.sign.orEmpty().ifBlank { "这个人很神秘，什么都没有写" },
        )

        Spacer(Modifier.height(8.dp))
        SpaceProfileCharge(content, actions)

        Spacer(Modifier.height(4.dp))
        Text(
            text = "UID：$mid",
            color = colors.onBackgroundVariant,
            fontSize = 10.5.sp,
        )
    }
}

@Composable
internal fun SpaceProfileNameActions(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val colors = MiuixTheme.colorScheme
    val mid = content.mid
    val isSelf = content.isSelf
    val profile = content.snapshot.profile.space?.profile
    val followed = content.snapshot.profile.followed
    val followBusy = content.snapshot.profile.followBusy
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = profile?.name.orEmpty(), color = colors.onBackground,
            fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
            MessageCircleButton(
                onClick = {
                    context.startActivity(
                        createRouteIntent(context,
                            com.aritxonly.bdfun.core.navigation.Route.PrivateChat(mid, name = profile?.name.orEmpty(),
                                avatar = profile?.face.orEmpty())),
                    )
                },
            )
            FollowCapsule(
                isSelf = isSelf,
                followed = followed,
                busy = followBusy,
                modifier = Modifier.width(124.dp),
                onClick = actions.onFollow,
            )
    }
}

@Composable
internal fun SpaceOfficialProfile(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val colors = MiuixTheme.colorScheme
    val profile = content.snapshot.profile.space?.profile
    val verify = listOfNotNull(
        profile?.officialTitle?.takeIf { it.isNotBlank() },
        profile?.officialDesc?.takeIf { it.isNotBlank() },
    ).joinToString(" ")
    if (verify.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppGlyph(MiuixIcons.Ok, size = 11.dp, tint = Color(0xFF3B82F6))
            Spacer(Modifier.width(4.dp))
            Text(
                text = verify,
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SpaceProfileCharge(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val mid = content.mid
    val colors = MiuixTheme.colorScheme
    val profile = content.snapshot.profile.space?.profile
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(interactionSource = null, indication = null) {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(
                                "https://space.bilibili.com/$mid/upower/rank",
                            ),
                        ),
                    )
                }.onFailure { context.showToast("无法打开充电页") }
            }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChargeGlyph(size = 14.dp)
        Spacer(Modifier.width(4.dp))
        val charging = profile?.chargingCount ?: 0L
        Text(
            text = "${formatCountNoDecimal(charging)}人已为其充电",
            color = colors.onBackgroundVariant,
            fontSize = 11.5.sp,
        )
    }
}
