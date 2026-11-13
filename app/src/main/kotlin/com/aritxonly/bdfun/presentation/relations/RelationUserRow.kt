package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.user.RelationUser
import com.aritxonly.bdfun.ui.*
import com.aritxonly.teak.common.chrome.*
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.aritxonly.bdfun.feature.relations.application.*

@Composable
internal fun RelationUserRow(user: RelationUser, busy: Boolean, showAction: Boolean,
    onOpen: () -> Unit, onFollow: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.weight(1f).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(model = rememberImageRequest(user.face), contentDescription = "${user.name}的头像",
                contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surface))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.name, color = colors.onSurfaceContainer, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(user.sign.ifBlank { "这个人很神秘，什么都没有写" }, color = colors.onBackgroundVariant,
                    fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (showAction) Button(onClick = onFollow, enabled = !busy, minWidth = 76.dp, minHeight = 38.dp,
            cornerRadius = 24.dp, insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            colors = if (user.followed || user.attribute == null) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColorsPrimary()) {
            Text(when { busy -> "处理中"; user.attribute == null -> "重试状态"; user.attribute == 6 -> "已互关";
                user.followed -> "已关注"; else -> "+ 关注" }, fontSize = 12.sp,
                color = if (user.followed || user.attribute == null) colors.onSecondaryVariant else colors.onPrimary)
        }
    }
}
