/*
 * AboutAppHeader layout adapted from Deadliner (MIT), Copyright (c) 2024 Aritx Zhou.
 * Source revision and complete license: docs/SETTINGS_DEADLINER_PROVENANCE.md.
 */
package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BuildConfig
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 保留 Deadliner 252dp 身份区、96dp 图标和版本层级；产品信息由 bdFun 提供。 */
@Composable
internal fun SettingsAboutAppHeader(appName: String, onCheckUpdate: () -> Unit) {
    val context = LocalContext.current
    // 启动图标是 AdaptiveIconDrawable，painterResource 不支持该类型。
    val icon = remember(context) { context.applicationInfo.loadIcon(context.packageManager).toBitmap(288, 288).asImageBitmap() }
    Column(Modifier.fillMaxWidth().heightIn(min = 252.dp)
        .clickable(role = Role.Button, onClickLabel = "检查更新", onClick = onCheckUpdate)
        .padding(top = 28.dp, bottom = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(icon, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)))
        Text(appName, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
        Text("v${BuildConfig.VERSION_NAME}", color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        Spacer(Modifier.height(12.dp))
    }
}
